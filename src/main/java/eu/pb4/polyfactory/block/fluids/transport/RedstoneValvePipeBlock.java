package eu.pb4.polyfactory.block.fluids.transport;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.RedstoneConnectable;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;

public class RedstoneValvePipeBlock extends PipeBaseBlock implements ConfigurableBlock, RedstoneConnectable {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;
    private static final List<BlockConfig<?>> WRENCH_ACTIONS = List.of(BlockConfig.AXIS, BlockConfig.INVERTED);
    public RedstoneValvePipeBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false).setValue(INVERTED, false));
    }
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var state = this.defaultBlockState();
        return waterLog(ctx, state.setValue(AXIS, ctx.getClickedFace().getAxis()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS, POWERED, INVERTED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updatePowered(world, pos, state);
        }
        super.onPlace(state,world,pos, oldState, notify);
    }

    private void updatePowered(Level world, BlockPos pos, BlockState state) {
        boolean powered = world.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            world.setBlock(pos, state.setValue(POWERED, powered), 4);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        this.updatePowered(world, pos, state);
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    public EnumSet<Direction> getFlowDirections(BlockState state) {
        if (state.getValue(POWERED) == state.getValue(INVERTED)) {
            var axis = state.getValue(AXIS);
            return EnumSet.of(Direction.get(Direction.AxisDirection.POSITIVE, axis), Direction.get(Direction.AxisDirection.NEGATIVE, axis));
        }
        return EnumSet.noneOf(Direction.class);
    }

    @Override
    public Collection<BlockNode> createPipeNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return state.getValue(POWERED) == state.getValue(INVERTED) ? super.createPipeNodes(state, world, pos) : List.of();
    }

    @Override
    public boolean checkModelDirection(BlockState state, Direction direction) {
        return state.getValue(AXIS) == direction.getAxis();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.rotateAxis(state, AXIS, rotation);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneValvePipeBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return RedstoneValvePipeBlockEntity::tick;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return dir == null || state.getValue(AXIS) != dir.getAxis();
    }

    public static final class Model extends BlockModel {
        private final ItemDisplayElement main;
        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSolid(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2f));

            this.updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(AXIS);
            float p = -90;
            float y = 0;

            if (dir != Direction.Axis.Y) {
                p = 0;
                y = dir == Direction.Axis.X ? 90 : 0;
            }

            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                this.tick();
            }
        }
    }
}
