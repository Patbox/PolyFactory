package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.block.property.ConnectablePart;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.fluid.SidedMultiFluidViewModel;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class FluidTankBlock extends Block implements FactoryBlock, PipeConnectable, EntityBlock {
    public static final EnumProperty<ConnectablePart> PART_X = FactoryProperties.CONNECTABLE_PART_X;
    public static final EnumProperty<ConnectablePart> PART_Y = FactoryProperties.CONNECTABLE_PART_Y;
    public static final EnumProperty<ConnectablePart> PART_Z = FactoryProperties.CONNECTABLE_PART_Z;
    public FluidTankBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(PART_X, ConnectablePart.SINGLE).setValue(PART_Y, ConnectablePart.SINGLE).setValue(PART_Z, ConnectablePart.SINGLE));
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof FilledStateProvider be) {
            return (int) ((be.getFilledAmount() * 15) / be.getFillCapacity());
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var x = getBlockStateAt(ctx.getLevel(), ctx.getClickedPos());

        if (ctx.getPlayer() instanceof ServerPlayer player && (!x.getValue(PART_X).single() || !x.getValue(PART_Y).single() || !x.getValue(PART_Z).single())) {
            TriggerCriterion.trigger(player, FactoryTriggers.FLUID_TANK_CONNECT);
        }

        return x;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return getBlockStateAt(world, pos);
    }

    private BlockState getBlockStateAt(LevelReader world, BlockPos pos) {
        var x = ConnectablePart.SINGLE;
        var y = ConnectablePart.SINGLE;
        var z = ConnectablePart.SINGLE;
        if (world.getBlockState(pos.below()).is(this)) {
            y = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.above()).is(this)) {
            y = y == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }

        if (world.getBlockState(pos.north()).is(this)) {
            z = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.south()).is(this)) {
            z = z == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }

        if (world.getBlockState(pos.west()).is(this)) {
            x = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.east()).is(this)) {
            x = x == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }


        return this.defaultBlockState().setValue(PART_X, x).setValue(PART_Y, y).setValue(PART_Z, z);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART_X, PART_Y, PART_Z);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return FluidTankBlockEntity::tick;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_BLOCK.defaultBlockState();
    }


    public static final class Model extends BlockModel {
        private final ItemDisplayElement main;
        private final SidedMultiFluidViewModel fluid;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(FactoryModels.BLOCK_FLUID_TANK.get(state));
            this.main.setScale(new Vector3f(2f));
            this.main.setYaw(180);
            this.fluid = new SidedMultiFluidViewModel(this, state.getValue(PART_X), state.getValue(PART_Z));
            this.addElement(this.main);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            super.notifyUpdate(updateType);
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                this.fluid.update(state.getValue(PART_X), state.getValue(PART_Z));
                this.main.setItem(FactoryModels.BLOCK_FLUID_TANK.get(state));
            }
        }

        public void setFluids(FluidContainer container) {
            this.fluid.setFluids(container.topFluid(), container.bottomFluid(), container::provideRender, container::doesNotContain);
        }

        public void setFluidAbove(@Nullable FluidInstance<?> fluidInstance) {
            this.fluid.setFluidAbove(fluidInstance);
        }

        public void setFluidBelow(@Nullable FluidInstance<?> fluidInstance) {
            this.fluid.setFluidBelow(fluidInstance);
        }
    }
}
