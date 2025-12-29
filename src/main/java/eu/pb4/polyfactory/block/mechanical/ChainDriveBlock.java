package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.entity.ChainLiftEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.ChainItemDisplayElement;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.AxisWithDirectNode;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.*;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class ChainDriveBlock extends AxleBlock implements EntityBlock {
    public static final float RADIUS = (12 - 3f) / 16f;
    public static final boolean CLICKABLE_CHAINS = false;
    public ChainDriveBlock(Properties settings) {
        super(settings);
    }

    public static int getChainCost(BlockPos start, BlockPos end, Direction.Axis startAxis, Direction.Axis endAxis) {
        var pos1 = Vec3.atCenterOf(start);
        var pos2 = Vec3.atCenterOf(end);

        var squaredDistance = pos1.distanceToSqr(pos2);

        var maxAngle = 33 * Mth.DEG_TO_RAD;

        var isCone = false;
        {
            var axisDist = Math.abs(pos1.get(startAxis) - pos2.get(startAxis));
            var dist = Mth.sqrt((float) (squaredDistance - axisDist * axisDist));

            var angle = Math.atan2(axisDist, dist);
            if (Math.abs(angle) > maxAngle) {
                isCone = true;
            }
        }
        {
            var axisDist = Math.abs(pos1.get(endAxis) - pos2.get(endAxis));
            var dist = Mth.sqrt((float) (squaredDistance - axisDist * axisDist));
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

        return Mth.ceil(Math.sqrt(squaredDistance) * 2);
    }

    public static void showPreview(ServerPlayer player, Level world, BlockPos start, BlockPos target) {
        if (player.tickCount % 5 == 0 || start.equals(target)
                || !(world.getBlockEntity(target) instanceof ChainDriveBlockEntity be)
                || !(world.getBlockEntity(start) instanceof ChainDriveBlockEntity be2)
                || be.hasConnection(start) || be2.hasConnection(target)
        ) {
            return;
        }

        var cost = getChainCost(start, target, be2.getBlockState().getValue(AXIS), be.getBlockState().getValue(AXIS));

        var stack = player.getMainHandItem().is(Items.IRON_CHAIN) ? player.getMainHandItem() : player.getOffhandItem();

        var hasEnoughChains = player.isCreative() || stack.getCount() >= cost;

        var origin = Vec3.atCenterOf(start);
        var offset = Vec3.atCenterOf(target).subtract(origin);
        var entry = Route.create(be2.getBlockState().getValue(AXIS), offset, be.getBlockState().getValue(AXIS));

        var color = cost > 0 ? (hasEnoughChains ? 0x00EE00 : 0xFFAA00) : 0xEE0000;

        var particle = new DustParticleOptions(color, 1f);

        var delta = 0.5d / entry.distance;

        player.displayClientMessage((switch (cost) {
            case -2 -> Component.translatable("block.polyfactory.chain_drive.invalid_too_far");
            case -1 -> Component.translatable("block.polyfactory.chain_drive.invalid_angle");
            default -> Component.translatable("block.polyfactory.chain_drive.required_chains", cost, stack.getHoverName());
        }).withColor(color), true);

        for (var x = 0d; x < 1; x += delta) {
            var particle1 = origin.add(entry.startPos.lerp(entry.startOffset, x));
            var particle2 = origin.add(entry.endOffset.lerp(entry.endPos, x));
            player.connection.send(new ClientboundLevelParticlesPacket(particle, false, false, particle1.x, particle1.y, particle1.z, 0, 0, 0, 0, 0));
            player.connection.send(new ClientboundLevelParticlesPacket(particle, false, false, particle2.x, particle2.y, particle2.z, 0, 0, 0, 0, 0));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public boolean placesLikeAxle() {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(FactoryItems.CHAIN_LIFT)) {
            return ChainLiftEntity.attach(stack, state, world, pos, player, hand, hit);
        }

        if (!stack.is(Items.IRON_CHAIN) || !(player instanceof ChainDriveHandler handler) || !(world.getBlockEntity(pos) instanceof ChainDriveBlockEntity be)) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }

        var start = handler.polyfactory$getCurrentChainStart();
        if (start == null || !(world.getBlockEntity(start) instanceof ChainDriveBlockEntity be2)) {
            handler.polyfactory$setCurrentChainStart(pos.immutable());
            return InteractionResult.SUCCESS_SERVER;
        }
        if (be == be2) {
            return InteractionResult.SUCCESS_SERVER;
        }
        var selfAxis = state.getValue(AXIS);
        var otherAxis = be2.getBlockState().getValue(AXIS);

        var cost = getChainCost(start, pos, otherAxis, selfAxis);

        if (cost <= 0 || (stack.getCount() < cost && !player.isCreative())) {
            return InteractionResult.CONSUME;
        }

        var result = player.isCreative() ? ItemStack.EMPTY : stack.split(cost);

        be.addConnection(start, otherAxis, result);
        be2.addConnection(pos, selfAxis, result.copy());
        handler.polyfactory$setCurrentChainStart(null);

        if (player instanceof ServerPlayer serverPlayer) {
            TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CHAIN_DRIVES_CONNECTED);
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (world.getBlockEntity(pos) instanceof ChainDriveBlockEntity be) {
            //noinspection deprecation
            be.setBlockState(state);
            be.updateAxis(pos, state.getValue(AXIS));
            for (var block : be.connections()) {
                if (world.getBlockEntity(block) instanceof ChainDriveBlockEntity be2) {
                    be2.updateAxis(pos, state.getValue(AXIS));
                }
            }
        }

    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChainDriveBlockEntity be) {
            return List.of(new AxisWithDirectNode(state.getValue(AXIS), List.copyOf(be.connections())));
        }

        return List.of(new SimpleAxisNode(state.getValue(AXIS)));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.mechanical.ChainDriveBlock.Model(world, initialBlockState, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChainDriveBlockEntity(pos, state);
    }

    public interface ChainDriveHandler {
        BlockPos polyfactory$getCurrentChainStart();

        void polyfactory$setCurrentChainStart(BlockPos pos);
    }

    public static final class Model extends RotationAwareModel {
        private static final ItemStack CHAIN = ItemDisplayElementUtil.getSolidModel(id("block/chain_drive_chain"));
        private static final ItemStack CHAIN_1 = ItemDisplayElementUtil.getSolidModel(id("block/chain_drive_chain_1"));
        private static final ItemStack CHAIN_2 = ItemDisplayElementUtil.getSolidModel(id("block/chain_drive_chain_2"));
        private final ItemDisplayElement mainElement;
        private final Map<BlockPos, Entry> entries = new HashMap<>();
        private float lastRotation = 0;

        private Model(ServerLevel world, BlockState state, BlockPos pos) {
            this.mainElement = LodItemDisplayElement.createSimple(ItemDisplayElementUtil.getSolidModel(state.getBlock().asItem()),
                    this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.updateAnimation(0, state.getValue(AXIS), pos);
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float rotation, Direction.Axis axis, BlockPos pos) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
            }

            mat.rotateY(rotation);
            mat.scale(2, 2f, 2);

            this.mainElement.setTransformation(mat);
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
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(),
                        this.blockAware().getBlockState().getValue(AXIS), this.blockPos());
                this.mainElement.startInterpolationIfDirty();
            }

            if (tick % 4 != 0) {
                return;
            }
            var dist = this.getRotationData().speedRadians() * 4 * RADIUS;
            var rotMax = (RotationConstants.MAX_ROTATION_PER_TICK_4 - Mth.DEG_TO_RAD * 10);

            if (Math.abs(dist) > rotMax) {
                this.lastRotation += rotMax * Math.signum(dist);
            } else {
                this.lastRotation += dist;
            }

            for (var pair : this.entries.entrySet()) {
                this.updateChains(pair.getValue());
            }
        }

        private void updateChains(Entry entry) {
            var route = entry.route;
            var x = this.lastRotation % route.doubleDistance;
            if (x < 0) {
                x += route.doubleDistance;
            }

            for (var item : entry.displays) {
                //item.setInterpolationDuration(4);
                item.setTeleportDuration(4);
                if (x < route.distance) {
                    item.setLeftRotation(route.rot);
                    item.setOffset(route.startPos.lerp(route.startOffset, x / route.distance));
                    //item.setTranslation(route.startPos.lerp(route.startOffset, x / route.distance).toVector3f());
                } else {
                    item.setLeftRotation(route.rot2);
                    item.setOffset(route.endOffset.lerp(route.endPos, x / route.distance - 1));
                    //item.setTranslation(route.endOffset.lerp(route.endPos, x / route.distance - 1).toVector3f());
                }
                //item.startInterpolationIfDirty();

                item.forceSync();

                x = (x + 0.5) % route.doubleDistance;
            }
        }

        public void setConnections(Map<BlockPos, Route> routes) {
            for (var key : List.copyOf(this.entries.keySet())) {
                this.removeConnection(key);
            }
            for (var con : routes.entrySet()) {
                this.addConnection(con.getKey(), con.getValue());
            }
        }

        public void addConnection(BlockPos pos, Route route) {
            var otherPos = Vec3.atCenterOf(pos);
            if (otherPos.lengthSqr() > this.getPos().lengthSqr()) {
                return;
            }
            var list = new ArrayList<ChainItemDisplayElement>();
            var interactions = new ArrayList<InteractionElement>();

            var offset = otherPos.subtract(this.getPos());
            var distance = offset.length();
            var doubleDistance = distance * 2;

            var middle = otherPos.lerp(this.getPos(), 0.5);

            int i = 0;
            for (var x = 0d; x <= doubleDistance; x += 0.5f) {
                var item = ChainItemDisplayElement.create(i++ % 2 == 0 ? CHAIN_1 : CHAIN_2, 4, this.getPos(), middle, otherPos);
                //item.setDisplaySize(0, 0);
                item.setViewRange(0.65f);
                item.setScale(new Vector3f(2));
                list.add(item);
            }

            if (CLICKABLE_CHAINS) {
                for (var x = RADIUS / 4; x < distance; x += RADIUS /2 ) {
                    var item = new InteractionElement();
                    item.setSize(RADIUS / 4, RADIUS / 4);
                    item.setOffset(Vec3.ZERO.lerp(offset, x / distance).subtract(0, RADIUS / 8, 0));
                    interactions.add(item);
                    this.addElement(item);
                }
            }

            var entry = new Entry(list, interactions, route);
            this.updateChains(entry);
            list.forEach(this::addElement);

            this.entries.put(pos.immutable(), entry);
        }

        public void removeConnection(BlockPos pos) {
            var old = this.entries.remove(pos);
            if (old != null) {
                old.displays.forEach(this::removeElement);
            }
        }

        private record Entry(List<ChainItemDisplayElement> displays, List<InteractionElement> interactions, Route route) {
        }
    }

    public record Route(Quaternionf rot, Quaternionf rot2,
                        Vec3 startPos, Vec3 endPos, Vec3 startOffset, Vec3 endOffset,
                        double doubleDistance, double distance, Quaternionf facingRotFrom, Quaternionf facingRotTo) {

        public static Route create(Direction.Axis selfAxis, BlockPos selfPos, Direction.Axis targetAxis, BlockPos targetPos) {
            return create(selfAxis, Vec3.atLowerCornerOf(targetPos.subtract(selfPos)), targetAxis);
        }

        public static Route create(Direction.Axis selfAxis, Vec3 selfPos, Direction.Axis targetAxis, Vec3 targetPos) {
            return create(selfAxis, targetPos.subtract(selfPos), targetAxis);
        }

        public static Route create(Direction.Axis selfAxis, Vec3 offset, Direction.Axis targetAxis) {
            var vec = offset.normalize().toVector3f();

            var facingRotFrom = switch (selfAxis) {
                case Y -> Direction.UP.getRotation().rotateY((float) Math.atan2(vec.x, vec.z) - Mth.HALF_PI);
                case X -> Direction.EAST.getRotation().rotateY((float) Math.atan2(vec.y, -vec.z));
                case Z -> Direction.SOUTH.getRotation().rotateY((float) Math.atan2(vec.y, vec.x));
            };

            if (!facingRotFrom.isFinite()) {
                facingRotFrom = new Quaternionf();
            }


            var startPos = new Vec3(new Vector3f(0, 0, RADIUS).rotate(facingRotFrom));
            var endPos = new Vec3(new Vector3f(0, 0, -RADIUS).rotate(facingRotFrom));

            var facingRotTo = selfAxis == targetAxis ? facingRotFrom : switch (targetAxis) {
                case Y -> Direction.UP.getRotation().rotateY((float) Math.atan2(vec.x, vec.z) - Mth.HALF_PI);
                case X -> Direction.EAST.getRotation().rotateY((float) Math.atan2(vec.y, -vec.z));
                case Z -> Direction.SOUTH.getRotation().rotateY((float) Math.atan2(vec.y, vec.x));
            };

            if (!facingRotTo.isFinite()) {
                facingRotTo = new Quaternionf();
            }

            var offsetOuterStart = new Vector3f(0, 0, RADIUS).rotate(facingRotTo);
            var offsetOuterEnd = new Vector3f(0, 0, -RADIUS).rotate(facingRotTo);

            var startOffset = offset.add(offsetOuterStart.x, offsetOuterStart.y, offsetOuterStart.z);
            var endOffset = offset.add(offsetOuterEnd.x, offsetOuterEnd.y, offsetOuterEnd.z);

            vec.set(startPos.toVector3f());
            var rot = new Quaternionf().rotateTo(Direction.UP.step(), startOffset.subtract(startPos).normalize().toVector3f());
            var rot2 = new Quaternionf().rotateTo(Direction.UP.step(), endPos.subtract(endOffset).normalize().toVector3f());
            var l = offset.length();

            return new Route(rot, rot2, startPos, endPos, startOffset, endOffset, l * 2, l, facingRotFrom, facingRotTo);
        }
    }
}
