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
import eu.pb4.polyfactory.block.configurable.WrenchModifyBlockValue;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class EjectorBlock extends RotationalNetworkBlock implements FactoryBlock, RotationUser, EntityBlock, ConfigurableBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    private static final List<BlockConfig<?>> WRENCH_ACTIONS = List.of(
            BlockConfig.FACING_HORIZONTAL,
            BlockConfig.ofBlockEntity("angle", Codec.FLOAT, EjectorBlockEntity.class,
                    (x, world, pos, side, state) -> Component.literal(String.format(Locale.ROOT, "%.0f", x)),
                    EjectorBlockEntity::angle, EjectorBlockEntity::setAngle,
                    WrenchModifyBlockValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 5f : -5f), 10, 65))),
            BlockConfig.ofBlockEntity("strength", Codec.FLOAT, EjectorBlockEntity.class,
                    (x, world, pos, side, state) -> Component.literal(String.format(Locale.ROOT, "%.2f", x)),
                    EjectorBlockEntity::strength, EjectorBlockEntity::setStrength,
                    WrenchModifyBlockValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 0.25f : -0.25f), 1, 2.5f)))
            );


    public EjectorBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(ENABLED, true));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
        super.onPlace(state,world,pos, oldState, notify);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    private void updateEnabled(Level world, BlockPos pos, BlockState state) {
        boolean powered = world.hasNeighborSignal(pos);
        if (powered == state.getValue(ENABLED)) {
            world.setBlock(pos, state.setValue(ENABLED, !powered), 4);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(state.getValue(FACING).getClockWise().getAxis()));
    }

    public void onLandedUpon(Level world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!(world.getBlockEntity(pos) instanceof EjectorBlockEntity be) || (be.progress() < 1 && be.ignoredTick() + 4 < world.getGameTime()) || !state.getValue(ENABLED)) {
            return;
        }
        super.fallOn(world, state, pos, entity, fallDistance);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!(world.getBlockEntity(pos) instanceof EjectorBlockEntity be) || (be.progress() < 1 && be.ignoredTick() + 4 < world.getGameTime()) || !state.getValue(ENABLED)) {
            return;
        }

        if (be.progress() >= 1) {
            world.playSound(null, pos, FactorySoundEvents.BLOCK_EJECTOR_LAUNCH, SoundSource.BLOCKS);
            be.setIgnoredTick(world.getGameTime());
        }
        be.setProgress(0);


        var rot = state.getValue(FACING).getClockWise();

        var verticalDrag = 0.98f;
        var horizontalDrag = 0.98f;

        if (entity instanceof LivingEntity livingEntity) {
            horizontalDrag = livingEntity.shouldDiscardFriction() ? 1 : 0.91F;
        }
        if (entity instanceof FlyingAnimal ) {
            verticalDrag = horizontalDrag;
        }

        var vec = new Vec3(state.getValue(FACING).step()
                .mul(1 / verticalDrag, 1 / horizontalDrag, 1 / verticalDrag)
                .mul(be.strength())
                .mul((float) (entity.getGravity() / 0.08))
                .rotateAxis(be.angle() * Mth.DEG_TO_RAD, rot.getStepX(), rot.getStepY(), rot.getStepZ())
        );
        entity.setOnGround(false);
        entity.setDeltaMovement(vec);
        if (entity instanceof ServerPlayer player) {
            FactoryUtil.sendVelocityDelta(player, vec.add(0, player.getGravity(), 0));
            TriggerCriterion.trigger(player, FactoryTriggers.LAUNCHED_BY_EJECTOR);
        } else if (entity instanceof LastFanEffectedTickConsumer c) {
            c.polyfactory$setLastFanTick();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EjectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.EJECTOR ? EjectorBlockEntity::tick : null;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, FACING);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof EjectorBlockEntity be) {
            be.updateRotationalData(modifier, state, world, pos);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof EjectorBlockEntity be ? (int) (be.progress() * 15) : 0;
    }

    @Override
    public void wrenchTick(ServerPlayer player, BlockHitResult hit, BlockState state) {
        if (true) {
            return;
        }

        var world = player.level();
        if (!(world.getBlockEntity(hit.getBlockPos()) instanceof EjectorBlockEntity be)) {
            return;
        }
        var out = Vec3.upFromBottomCenterOf(hit.getBlockPos(), 1);

        var rot = state.getValue(FACING).getClockWise();
        var vec = new Vec3(state.getValue(FACING).step()
                .mul(1 / 0.98f, 1 / 0.98f, 1 / 0.98f)
                .mul(be.strength())
                .mul(0.5f)
                .rotateAxis(be.angle() * Mth.DEG_TO_RAD, rot.getStepX(), rot.getStepY(), rot.getStepZ())
        );

        int endTime = (player.tickCount / 4) % 10;

        var particle = new DustParticleOptions(0xFF0000, 1);

        for (int i = 0; i < 40; i++) {
            player.connection.send(new ClientboundLevelParticlesPacket(particle, true, true,
                    out.x, out.y, out.z, 0, 0, 0, 0, 0));
            vec = vec.add(0, -0.08, 0);
            out = out.add(vec);
            vec = vec.scale(0.98);
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
            var dir = state.getValue(FACING);
            float y = dir.toYRot();
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
            this.plate.startInterpolationIfDirty();
            this.link.startInterpolationIfDirty();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }
    }
}
