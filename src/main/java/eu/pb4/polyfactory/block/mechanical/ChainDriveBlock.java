package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.ChainDriveNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.*;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class ChainDriveBlock extends AxleBlock implements BlockEntityProvider {
    public ChainDriveBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
    }

    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext context) {
        return false;
    }

    @Override
    public void onPolymerBlockSend(BlockState blockState, BlockPos.Mutable pos, PacketContext.NotNullWithPlayer player) {}

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(Items.CHAIN) || !(player instanceof ChainDriveHandler handler) || !(world.getBlockEntity(pos) instanceof ChainDriveBlockEntity be)) {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        }

        var start = handler.polyfactory$getCurrentChainStart();
        if (start == null || !(world.getBlockEntity(start) instanceof ChainDriveBlockEntity be2)) {
            handler.polyfactory$setCurrentChainStart(pos.toImmutable());
            return ActionResult.SUCCESS_SERVER;
        }
        if (be == be2) {
            return ActionResult.SUCCESS_SERVER;
        }
        var selfAxis = state.get(AXIS);
        var otherAxis = be2.getCachedState().get(AXIS);

        var cost = getChainCost(start, pos, otherAxis, selfAxis);

        if (cost <= 0 || (stack.getCount() < cost && !player.isCreative())) {
            return ActionResult.CONSUME;
        }

        var result = player.isCreative() ? ItemStack.EMPTY : stack.split(cost);

        be.addConnection(start, otherAxis, result);
        be2.addConnection(pos, selfAxis, result.copy());
        handler.polyfactory$setCurrentChainStart(null);

        return ActionResult.SUCCESS_SERVER;
    }

    private static int getChainCost(BlockPos start, BlockPos end, Direction.Axis startAxis, Direction.Axis endAxis) {
        var pos1 = Vec3d.ofCenter(start);
        var pos2 = Vec3d.ofCenter(end);

        var squaredDistance = pos1.squaredDistanceTo(pos2);

        var maxAngle = 33 * MathHelper.RADIANS_PER_DEGREE;

        var isCone = false;
        {
            var axisDist = Math.abs(pos1.getComponentAlongAxis(startAxis) - pos2.getComponentAlongAxis(startAxis));
            var dist = MathHelper.sqrt((float) (squaredDistance - axisDist * axisDist));

            var angle = Math.atan2(axisDist, dist);
            if (Math.abs(angle) > maxAngle) {
                isCone = true;
            }
        }
        {
            var axisDist = Math.abs(pos1.getComponentAlongAxis(endAxis) - pos2.getComponentAlongAxis(endAxis));
            var dist = MathHelper.sqrt((float) (squaredDistance - axisDist * axisDist));
            var angle = Math.atan2(axisDist, dist);
            if (Math.abs(angle) > maxAngle) {
                isCone = true;
            }
        }

        if (isCone) {
            return -1;
        }

        if (squaredDistance > 32 * 32) {
            return -2;
        }

        return MathHelper.ceil(Math.sqrt(squaredDistance) * 2);
    }

    public static void showPreview(ServerPlayerEntity player, World world, BlockPos start, BlockPos target) {
        if (player.age % 5 == 0 || start.equals(target)
                || !(world.getBlockEntity(target) instanceof ChainDriveBlockEntity be)
                || !(world.getBlockEntity(start) instanceof ChainDriveBlockEntity be2)
                || be.getConnection().containsKey(start) || be2.getConnection().containsKey(target)
        ) {
            return;
        }

        var cost = getChainCost(start, target, be2.getCachedState().get(AXIS), be.getCachedState().get(AXIS));

        var stack = player.getMainHandStack().isOf(Items.CHAIN) ? player.getMainHandStack() : player.getOffHandStack();

        var hasEnoughChains = player.isCreative() || stack.getCount() >= cost;

        var origin = Vec3d.ofCenter(start);
        var offset = Vec3d.ofCenter(target).subtract(origin);
        var entry = Model.createEntry(List.of(), be2.getCachedState().get(AXIS), offset, be.getCachedState().get(AXIS));

        var color = cost > 0 ? (hasEnoughChains ? 0x00EE00 : 0xFFAA00) : 0xEE0000;

        var particle = new DustParticleEffect(color, 1f);

        var delta = 0.5d / entry.distance;

        player.sendMessage((switch (cost) {
            case -2 -> Text.translatable("block.polyfactory.chain_drive.invalid_too_far");
            case -1 -> Text.translatable("block.polyfactory.chain_drive.invalid_angle");
            default -> Text.translatable("block.polyfactory.chain_drive.required_chains", cost);
        }).withColor(color), true);

        for (var x = 0d; x < 1; x += delta) {
            var particle1 = origin.add(entry.startPos.lerp(entry.startOffset, x));
            var particle2 = origin.add(entry.endOffset.lerp(entry.endPos, x));
            player.networkHandler.sendPacket(new ParticleS2CPacket(particle, false, false, particle1.x, particle1.y, particle1.z, 0, 0,0, 0, 0));
            player.networkHandler.sendPacket(new ParticleS2CPacket(particle, false, false, particle2.x, particle2.y, particle2.z, 0, 0,0, 0, 0));
        }
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (world.getBlockEntity(pos) instanceof ChainDriveBlockEntity be) {
            be.updateAxis(pos, state.get(AXIS));
            for (var block : be.getConnection().keySet()) {
                if (world.getBlockEntity(block) instanceof ChainDriveBlockEntity be2) {
                    be2.updateAxis(pos, state.get(AXIS));
                }
            }
        }

    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChainDriveBlockEntity be) {
            return List.of(new ChainDriveNode(state.get(AXIS), List.copyOf(be.getConnection().keySet())));
        }

        return List.of(new SimpleAxisNode(state.get(AXIS)));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState, pos);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChainDriveBlockEntity(pos, state);
    }

    public static final class Model extends RotationAwareModel {
        private static final ItemStack CHAIN_1 = ItemDisplayElementUtil.getModel(id("block/chain_drive_chain_1"));
        private static final ItemStack CHAIN_2 = ItemDisplayElementUtil.getModel(id("block/chain_drive_chain_2"));
        private final ItemDisplayElement mainElement;
        private final Map<BlockPos, Entry> entries = new HashMap<>();
        private float lastRotation = 0;
        private Model(ServerWorld world, BlockState state, BlockPos pos) {
            this.mainElement = LodItemDisplayElement.createSimple(ItemDisplayElementUtil.getModel(state.getBlock().asItem()),
                    this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.updateAnimation(0,  state.get(AXIS));
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.SOUTH.getRotationQuaternion());
            }

            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);

            this.mainElement.setTransformation(mat);

            var dist = this.getRotationData().speed() * (this.getRotationData().isNegative() ? 1 : -1) * MathHelper.RADIANS_PER_DEGREE * this.getUpdateRate() * 0.4f;
            var rotMax = (RotationConstants.MAX_ROTATION_PER_TICK_4 - MathHelper.RADIANS_PER_DEGREE * 15);

            if (Math.abs(dist) > rotMax) {
                this.lastRotation += (float) (rotMax * Math.signum(dist));
            } else {
                this.lastRotation += (float) dist;
            }

            for (var pair : this.entries.entrySet()) {
                this.updateChains(pair.getValue());
            }
        }

        private void updateChains(Entry entry) {
            var x = this.lastRotation % entry.doubleDistance;
            if (x < 0) {
                x += entry.doubleDistance;
            }

            for (var item : entry.displays) {
                // Todo: optimize packet count
                item.setTeleportDuration(this.getUpdateRate());
                if (x < entry.distance) {
                    item.setLeftRotation(entry.rot);
                    item.setOffset(entry.startPos.lerp(entry.startOffset, x / entry.distance));
                } else {
                    item.setLeftRotation(entry.rot2);
                    item.setOffset(entry.endOffset.lerp(entry.endPos, x / entry.distance - 1));
                }

                if (item.getDataTracker().isDirty(DisplayTrackedData.LEFT_ROTATION)) {
                    item.updateLastSyncedPos();
                    this.sendPacket(new EntityPositionSyncS2CPacket(item.getEntityId(), new PlayerPosition(item.getLastSyncedPos(), Vec3d.ZERO, 0, 0), false));
                }

                x = (x + 0.5) % entry.doubleDistance;
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                //var pos = this.blockAware().getBlockPos();
                //this.mainElement.setItem(((pos.getX() + pos.getY() + pos.getZ()) % 2 == 0) ? ITEM_MODEL_2 : ITEM_MODEL_1);
            }
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(),
                        this.blockAware().getBlockState().get(AXIS));
                this.mainElement.startInterpolationIfDirty();
            }
        }

        public void setConnections(Map<BlockPos, ChainDriveBlockEntity.Entry> connectedBlockPos) {
            for (var key : List.copyOf(this.entries.keySet())) {
                this.removeConnection(key);
            }
            var axis = this.blockState().get(AXIS);
            for (var con : connectedBlockPos.entrySet()) {
                this.addConnection(axis, con.getKey(), con.getValue().axis());
            }
        }

        public void addConnection(Direction.Axis selfAxis, BlockPos pos, Direction.Axis targetAxis) {
            var otherPos = Vec3d.ofCenter(pos);
            if (otherPos.lengthSquared() > this.getPos().lengthSquared()) {
                return;
            }
            var list = new ArrayList<ItemDisplayElement>();

            var offset = otherPos.subtract(this.getPos());
            var doubleDistance = offset.length() * 2;

            int i = 0;
            for (var x = 0d; x <= doubleDistance; x += 0.5) {
                var item = ItemDisplayElementUtil.createSimple(i++ % 2 == 0 ? CHAIN_1 : CHAIN_2, 3);
                item.setScale(new Vector3f(2));
                list.add(item);
            }

            var entry = createEntry(list, selfAxis, offset, targetAxis);
            this.updateChains(entry);
            list.forEach(this::addElement);

            this.entries.put(pos.toImmutable(), entry);
        }
        private static Entry createEntry(List<ItemDisplayElement> list, Direction.Axis selfAxis, Vec3d offset, Direction.Axis targetAxis) {
            var vec = offset.normalize().toVector3f();

            var facingRotFrom = switch (selfAxis) {
                case Y -> Direction.UP.getRotationQuaternion().rotateY((float) Math.atan2(vec.x, vec.z) - MathHelper.HALF_PI);
                case X -> Direction.EAST.getRotationQuaternion().rotateY((float) Math.atan2(vec.y, -vec.z));
                case Z -> Direction.SOUTH.getRotationQuaternion().rotateY((float) Math.atan2(vec.y, vec.x));
            };

            if (!facingRotFrom.isFinite()) {
                facingRotFrom = new Quaternionf();
            }

            var shift = 0.4f;

            var startPos = new Vec3d(new Vector3f(0, 0, -shift).rotate(facingRotFrom));
            var endPos = new Vec3d(new Vector3f(0, 0, shift).rotate(facingRotFrom));

            var facingRotTo = selfAxis == targetAxis ? facingRotFrom : switch (targetAxis) {
                case Y -> Direction.UP.getRotationQuaternion().rotateY((float) Math.atan2(vec.x, vec.z) - MathHelper.HALF_PI);
                case X -> Direction.EAST.getRotationQuaternion().rotateY((float) Math.atan2(vec.y, -vec.z));
                case Z -> Direction.SOUTH.getRotationQuaternion().rotateY((float) Math.atan2(vec.y, vec.x));
            };

            if (!facingRotTo.isFinite()) {
                facingRotTo = new Quaternionf();
            }

            var offsetOuterStart = new Vector3f(0, 0, -shift).rotate(facingRotTo);
            var offsetOuterEnd = new Vector3f(0, 0, shift).rotate(facingRotTo);

            var startOffset = offset.add(offsetOuterStart.x, offsetOuterStart.y, offsetOuterStart.z);
            var endOffset = offset.add(offsetOuterEnd.x, offsetOuterEnd.y, offsetOuterEnd.z);

            vec.set(startPos.toVector3f());
            var rot = new Quaternionf().rotateTo(Direction.UP.getUnitVector(), startOffset.subtract(startPos).normalize().toVector3f());
            var rot2 = new Quaternionf().rotateTo(Direction.UP.getUnitVector(), endPos.subtract(endOffset).normalize().toVector3f());
            var l = offset.length();

            return new Entry(list, rot, rot2, startPos, endPos, startOffset, endOffset, l * 2, l);
        }


        public void removeConnection(BlockPos pos) {
            var old = this.entries.remove(pos);
            if (old != null) {
                old.displays.forEach(this::removeElement);
            }
        }

        private record Entry(List<ItemDisplayElement> displays, Quaternionf rot, Quaternionf rot2,
                             Vec3d startPos, Vec3d endPos, Vec3d startOffset, Vec3d endOffset,
                             double doubleDistance, double distance) {}
    }

    public interface ChainDriveHandler {
        BlockPos polyfactory$getCurrentChainStart();
        void polyfactory$setCurrentChainStart(BlockPos pos);
    }
}
