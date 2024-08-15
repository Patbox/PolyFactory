package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.property.ConnectablePart;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.fluid.MultiFluidViewModel;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class FluidTankBlock extends Block implements FactoryBlock, PipeConnectable, BlockEntityProvider {
    public static final EnumProperty<ConnectablePart> PART_X = FactoryProperties.CONNECTABLE_PART_X;
    public static final EnumProperty<ConnectablePart> PART_Y = FactoryProperties.CONNECTABLE_PART_Y;
    public static final EnumProperty<ConnectablePart> PART_Z = FactoryProperties.CONNECTABLE_PART_Z;
    public FluidTankBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(PART_X, ConnectablePart.SINGLE).with(PART_Y, ConnectablePart.SINGLE).with(PART_Z, ConnectablePart.SINGLE));
    }

    @Override
    public boolean canPipeConnect(WorldAccess world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getBlockStateAt(ctx.getWorld(), ctx.getBlockPos());
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return getBlockStateAt(world, pos);
    }

    private BlockState getBlockStateAt(WorldAccess world, BlockPos pos) {
        var x = ConnectablePart.SINGLE;
        var y = ConnectablePart.SINGLE;
        var z = ConnectablePart.SINGLE;
        if (world.getBlockState(pos.down()).isOf(this)) {
            y = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.up()).isOf(this)) {
            y = y == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }

        if (world.getBlockState(pos.north()).isOf(this)) {
            z = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.south()).isOf(this)) {
            z = z == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }

        if (world.getBlockState(pos.west()).isOf(this)) {
            x = ConnectablePart.POSITIVE;
        }

        if (world.getBlockState(pos.east()).isOf(this)) {
            x = x == ConnectablePart.POSITIVE ? ConnectablePart.MIDDLE : ConnectablePart.NEGATIVE;
        }


        return this.getDefaultState().with(PART_X, x).with(PART_Y, y).with(PART_Z, z);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PART_X, PART_Y, PART_Z);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return FluidTankBlockEntity::tick;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.COPPER_BLOCK.getDefaultState();
    }


    public static final class Model extends BlockModel {
        private final ItemDisplayElement main;
        private final MultiFluidViewModel fluid;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(FactoryModels.BLOCK_FLUID_TANK.get(state));
            this.main.setScale(new Vector3f(2f));
            this.main.setYaw(180);
            this.fluid = new MultiFluidViewModel(this, state.get(PART_X), state.get(PART_Z));
            this.addElement(this.main);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            super.notifyUpdate(updateType);
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                this.fluid.update(state.get(PART_X), state.get(PART_Z));
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
