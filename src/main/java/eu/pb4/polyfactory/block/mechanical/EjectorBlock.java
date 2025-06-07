package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.configurable.WrenchModifyValue;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class EjectorBlock extends RotationalNetworkBlock implements FactoryBlock, RotationUser, BlockEntityProvider, ConfigurableBlock {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = Properties.ENABLED;
    private static final List<BlockConfig<?>> WRENCH_ACTIONS = List.of(
            BlockConfig.FACING_HORIZONTAL,
            BlockConfig.ofBlockEntity("angle", Codec.FLOAT, EjectorBlockEntity.class,
                    (x, world, pos, side, state) -> Text.literal(String.format(Locale.ROOT, "%.0f", x)),
                    EjectorBlockEntity::angle, EjectorBlockEntity::setAngle,
                    WrenchModifyValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 5f : -5f), 10, 65))),
            BlockConfig.ofBlockEntity("strength", Codec.FLOAT, EjectorBlockEntity.class,
                    (x, world, pos, side, state) -> Text.literal(String.format(Locale.ROOT, "%.2f", x)),
                    EjectorBlockEntity::strength, EjectorBlockEntity::setStrength,
                    WrenchModifyValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 0.25f : -0.25f), 1, 2.5f)))
            );


    public EjectorBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ENABLED, true));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
        super.onBlockAdded(state,world,pos, oldState, notify);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    private void updateEnabled(World world, BlockPos pos, BlockState state) {
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered == state.get(ENABLED)) {
            world.setBlockState(pos, state.with(ENABLED, !powered), 4);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(state.get(FACING).rotateYClockwise().getAxis()));
    }

    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!(world.getBlockEntity(pos) instanceof EjectorBlockEntity be) || (be.progress() < 1 && be.ignoredTick() + 4 < world.getTime()) || !state.get(ENABLED)) {
            return;
        }
        super.onLandedUpon(world, state, pos, entity, fallDistance);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!(world.getBlockEntity(pos) instanceof EjectorBlockEntity be) || (be.progress() < 1 && be.ignoredTick() + 4 < world.getTime()) || !state.get(ENABLED)) {
            return;
        }

        if (be.progress() >= 1) {
            world.playSound(null, pos, FactorySoundEvents.BLOCK_EJECTOR_LAUNCH, SoundCategory.BLOCKS);
            be.setIgnoredTick(world.getTime());
        }
        be.setProgress(0);


        var rot = state.get(FACING).rotateYClockwise();

        var verticalDrag = 0.98f;
        var horizontalDrag = 0.98f;

        if (entity instanceof LivingEntity livingEntity) {
            horizontalDrag = livingEntity.hasNoDrag() ? 1 : 0.91F;
        }
        if (entity instanceof Flutterer ) {
            verticalDrag = horizontalDrag;
        }

        var vec = new Vec3d(state.get(FACING).getUnitVector()
                .mul(1 / verticalDrag, 1 / horizontalDrag, 1 / verticalDrag)
                .mul(be.strength())
                .mul((float) (entity.getFinalGravity() / 0.08))
                .rotateAxis(be.angle() * MathHelper.RADIANS_PER_DEGREE, rot.getOffsetX(), rot.getOffsetY(), rot.getOffsetZ())
        );
        entity.setOnGround(false);
        entity.setVelocity(vec);
        if (entity instanceof ServerPlayerEntity player) {
            FactoryUtil.sendVelocityDelta(player, vec.add(0, player.getFinalGravity(), 0));
            TriggerCriterion.trigger(player, FactoryTriggers.LAUNCHED_BY_EJECTOR);
        } else if (entity instanceof LastFanEffectedTickConsumer c) {
            c.polyfactory$setLastFanTick();
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EjectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.EJECTOR ? EjectorBlockEntity::tick : null;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.transform(state, mirror::apply, FACING);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof EjectorBlockEntity be) {
            be.updateRotationalData(modifier, state, world, pos);
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof EjectorBlockEntity be ? (int) (be.progress() * 15) : 0;
    }

    @Override
    public void wrenchTick(ServerPlayerEntity player, BlockHitResult hit, BlockState state) {
        if (true) {
            return;
        }

        var world = player.getWorld();
        if (!(world.getBlockEntity(hit.getBlockPos()) instanceof EjectorBlockEntity be)) {
            return;
        }
        var out = Vec3d.ofCenter(hit.getBlockPos(), 1);

        var rot = state.get(FACING).rotateYClockwise();
        var vec = new Vec3d(state.get(FACING).getUnitVector()
                .mul(1 / 0.98f, 1 / 0.98f, 1 / 0.98f)
                .mul(be.strength())
                .mul(0.5f)
                .rotateAxis(be.angle() * MathHelper.RADIANS_PER_DEGREE, rot.getOffsetX(), rot.getOffsetY(), rot.getOffsetZ())
        );

        int endTime = (player.age / 4) % 10;

        var particle = new DustParticleEffect(0xFF0000, 1);

        for (int i = 0; i < 40; i++) {
            player.networkHandler.sendPacket(new ParticleS2CPacket(particle, true, true,
                    out.x, out.y, out.z, 0, 0, 0, 0, 0));
            vec = vec.add(0, -0.08, 0);
            out = out.add(vec);
            vec = vec.multiply(0.98);
        }
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack BASE_MODEL = ItemDisplayElementUtil.getModel(id("block/ejector_base"));
        public static final ItemStack PLATE_MODEL = ItemDisplayElementUtil.getModel(id("block/ejector_plate"));
        public static final ItemStack LINK_MODEL = ItemDisplayElementUtil.getModel(id("block/ejector_link"));

        private final ItemDisplayElement base;
        private final ItemDisplayElement plate;
        private final ItemDisplayElement link;
        private float progress = -1;


        private Model(BlockState state) {
            this.base = ItemDisplayElementUtil.createSimple(BASE_MODEL);
            this.plate = ItemDisplayElementUtil.createSimple(PLATE_MODEL);
            this.plate.setInterpolationDuration(3);
            this.link = ItemDisplayElementUtil.createSimple(LINK_MODEL);
            this.link.setInterpolationDuration(3);
            this.updateStatePos(state);
            this.updateProgress(0);
            this.addElement(this.base);
            this.addElement(this.plate);
            this.addElement(this.link);

        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            float y = dir.getPositiveHorizontalDegrees();
            this.base.setYaw(y);
            this.plate.setYaw(y);
            this.link.setYaw(y);
        }

        public void updateProgress(float progress) {
            if (this.progress == progress) {
                return;
            }
            this.progress = progress;
            this.plate.setTransformation(mat().translate(0, 5 / 16f, 7 / 16f).rotateX(progress));
            this.link.setTransformation(mat().translate(0, 5 / 16f, 7 / 16f).rotateX(progress).translate(0, 0, -10 / 16f).rotateX(-progress / 2));
        }

        @Override
        protected void onTick() {
            if (this.plate.isDirty()) {
                this.plate.startInterpolation();
                this.link.startInterpolation();
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }
    }
}
