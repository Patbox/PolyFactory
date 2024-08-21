package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.fluid.SimpleMultiFluidViewModel;
import eu.pb4.polyfactory.util.FactoryUtil;
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
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import static eu.pb4.polyfactory.ModInit.id;

public class PortableFluidTankBlock extends Block implements FactoryBlock, PipeConnectable, BlockEntityProvider, BarrierBasedWaterloggable {
    public static final DirectionProperty FACING = Properties.FACING;
    public PortableFluidTankBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
        PortableFluidTankBlock.Model.BASE_MODEL.isEmpty();
    }

    @Override
    public boolean canPipeConnect(WorldAccess world, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING) == dir;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var sidePos = ctx.getBlockPos().offset(ctx.getSide(), -1);
        var state = ctx.getWorld().getBlockState(sidePos);
        if (state.getBlock() instanceof PipeConnectable connectable && connectable.canPipeConnect(ctx.getWorld(), sidePos, state, ctx.getSide())) {
            return this.getDefaultState().with(FACING, ctx.getSide().getOpposite());
        }
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getSide()));
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return state;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WATERLOGGED, FACING);
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
        return new PortableFluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return PortableFluidTankBlockEntity::tick;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.COPPER_BLOCK.getDefaultState();
    }

    public static final class Model extends BlockModel {
        public static final ItemStack BASE_MODEL = BaseItemProvider.requestModel(FactoryUtil.requestTransparentItem(), id("block/portable_fluid_tank"));
        public static final ItemStack WATERLOGGED_MODEL = BaseItemProvider.requestModel(FactoryUtil.requestTransparentItem(), id("block/portable_fluid_tank_waterlogged"));
        private final ItemDisplayElement main;
        private final SimpleMultiFluidViewModel fluid = new SimpleMultiFluidViewModel(this, FactoryModels.FLUID_PORTABLE_FLUID_TANK_VERTICAL, 16);

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.get(WATERLOGGED) ? WATERLOGGED_MODEL : BASE_MODEL);
            this.main.setScale(new Vector3f(2f));
            updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            float p = 0;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 90;
                y = dir.asRotation();
            } else if (dir == Direction.DOWN) {
                p = 180;
            }

            if (dir.getAxis() == Direction.Axis.Y) {
                this.fluid.setRotation(0, 0);
                this.fluid.setModels(FactoryModels.FLUID_PORTABLE_FLUID_TANK_VERTICAL, 16);
            } else {
                this.fluid.setRotation(0, dir.getAxis() == Direction.Axis.Z ? 90f : 0);
                this.fluid.setModels(FactoryModels.FLUID_PORTABLE_FLUID_TANK_HORIZONTAL, 12);
            }

            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        public void setFluid(FluidContainer container) {
            this.fluid.setFluids(container::provideRender, container::doesNotContain);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            super.notifyUpdate(updateType);
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                this.main.setItem(this.blockState().get(WATERLOGGED) ? WATERLOGGED_MODEL : BASE_MODEL);
                updateStatePos(this.blockState());
            }
        }
    }
}