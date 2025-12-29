package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.fluid.SimpleMultiFluidViewModel;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;

import static eu.pb4.polyfactory.ModInit.id;

public class PortableFluidTankBlock extends Block implements FactoryBlock, PipeConnectable, EntityBlock, BarrierBasedWaterloggable, ConfigurableBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public PortableFluidTankBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
        PortableFluidTankBlock.Model.BASE_MODEL.isEmpty();
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

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(FACING) == dir;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var sidePos = ctx.getClickedPos().relative(ctx.getClickedFace(), -1);
        var state = ctx.getLevel().getBlockState(sidePos);
        if (state.getBlock() instanceof PipeConnectable connectable && connectable.canPipeConnect(ctx.getLevel(), sidePos, state, ctx.getClickedFace())) {
            return this.defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
        }
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, ctx.getClickedFace()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED, FACING);
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
        return new PortableFluidTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return PortableFluidTankBlockEntity::tick;
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
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_BLOCK.defaultBlockState();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }

    public static final class Model extends BlockModel {
        public static final ItemStack BASE_MODEL = ItemDisplayElementUtil.getSolidModel(id("block/portable_fluid_tank"));
        public static final ItemStack WATERLOGGED_MODEL = ItemDisplayElementUtil.getSolidModel(id("block/portable_fluid_tank_waterlogged"));
        private final ItemDisplayElement main;
        private final SimpleMultiFluidViewModel fluid = new SimpleMultiFluidViewModel(this, FactoryModels.FLUID_PORTABLE_FLUID_TANK_VERTICAL, 16);

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getValue(WATERLOGGED) ? WATERLOGGED_MODEL : BASE_MODEL);
            this.main.setScale(new Vector3f(2f));
            this.main.setRightRotation(new Quaternionf().rotateX(Mth.HALF_PI));
            updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(FACING);
            float p = 0;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
            } else if (dir == Direction.DOWN) {
                p = 90;
            } else {
                p = -90;
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
                this.main.setItem(this.blockState().getValue(WATERLOGGED) ? WATERLOGGED_MODEL : BASE_MODEL);
                updateStatePos(this.blockState());
            }
        }
    }
}
