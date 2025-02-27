package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.WallShape;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipeInWallBlock extends PipeBaseBlock implements WrenchableBlock {
    public static final EnumProperty<Direction.Axis> AXIS = Properties.HORIZONTAL_AXIS;
    private static final List<WrenchAction> WRENCH_ACTIONS = List.of(WrenchAction.HORIZONTAL_AXIS);
    public static final IdentityHashMap<Block, PipeInWallBlock> MAP = new IdentityHashMap<>();
    private final WallBlock backing;

    public PipeInWallBlock(WallBlock wallBlock) {
        super(AbstractBlock.Settings.copy(wallBlock));
        this.backing = wallBlock;
        MAP.put(wallBlock, this);
    }

    public static BlockState fromWall(BlockState state, ItemUsageContext context) {
        var base = MAP.get(state.getBlock());
        if (base == null) {
            return null;
        }
        var axis = context.getSide().getAxis() == Direction.Axis.Y ? context.getHorizontalPlayerFacing().getAxis() : context.getSide().getAxis();
        return base.getDefaultState().with(WATERLOGGED, state.get(WATERLOGGED)).with(AXIS, axis);
    }

    public MutableText getName() {
        return Text.translatable("block.polyfactory.wall_with_pipe", this.backing.getName());
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return getPolymerBlockState(state);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        var a = state.get(AXIS) == Direction.Axis.X ? WallBlock.NORTH_SHAPE : WallBlock.WEST_SHAPE;
        var b = state.get(AXIS) == Direction.Axis.X ? WallBlock.SOUTH_SHAPE : WallBlock.EAST_SHAPE;

        return this.backing.getDefaultState().with(WATERLOGGED, state.get(WATERLOGGED)).with(WallBlock.UP, false).with(a, WallShape.TALL).with(b, WallShape.TALL);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = this.getDefaultState();
        var axis = ctx.getSide().getAxis() == Direction.Axis.Y ? ctx.getHorizontalPlayerFacing().getAxis() : ctx.getSide().getAxis();

        return waterLog(ctx, state.with(AXIS, axis));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return state;
    }

    public EnumSet<Direction> getFlowDirections(BlockState state) {
        var axis = state.get(AXIS);
        return EnumSet.of(Direction.get(Direction.AxisDirection.POSITIVE, axis), Direction.get(Direction.AxisDirection.NEGATIVE, axis));
    }

    @Override
    public boolean checkModelDirection(BlockState state, Direction direction) {
        return state.get(AXIS) == direction.getAxis();
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.rotateAxis(state, AXIS, rotation);
    }

    @Override
    public List<WrenchAction> getWrenchActions(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }

    public WallBlock getBacking() {
        return this.backing;
    }
}
