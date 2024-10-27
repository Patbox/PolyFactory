package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.block.property.LazyEnumProperty;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipeBlock extends PipeBaseBlock implements WrenchableBlock {

    public static final BooleanProperty LOCKED = FactoryProperties.LOCKED;
    public static final LazyEnumProperty<TriState> NORTH = FactoryProperties.TRI_STATE_NORTH;
    public static final LazyEnumProperty<TriState> SOUTH = FactoryProperties.TRI_STATE_SOUTH;

    public static final LazyEnumProperty<TriState> EAST = FactoryProperties.TRI_STATE_EAST;
    public static final LazyEnumProperty<TriState> WEST = FactoryProperties.TRI_STATE_WEST;
    public static final LazyEnumProperty<TriState> UP = FactoryProperties.TRI_STATE_UP;
    public static final LazyEnumProperty<TriState> DOWN = FactoryProperties.TRI_STATE_DOWN;
    private static final List<WrenchAction> WRENCH_ACTIONS = List.of(WrenchAction.of("locked", LOCKED));
    public static final Map<Direction, LazyEnumProperty<TriState>> FACING_PROPERTIES = FactoryProperties.TRI_STATE_DIRECTIONS;

    public PipeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(LOCKED, false).with(NORTH, TriState.FALSE).with(SOUTH, TriState.FALSE)
                .with(EAST, TriState.FALSE).with(WEST, TriState.FALSE).with(UP, TriState.FALSE).with(DOWN, TriState.FALSE));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = this.getDefaultState();
        int i = 0;
        Direction lastDir = null;
        for (var direction : Direction.values()) {
            var neighborPos = ctx.getBlockPos().offset(direction);
            if (canConnectTo(ctx.getWorld(), neighborPos, ctx.getWorld().getBlockState(neighborPos), direction.getOpposite())) {
                state = state.with(FACING_PROPERTIES.get(direction), TriState.TRUE);
                i++;
                lastDir = direction;
            }
        }
        if (i == 0) {
            state = state.with(FACING_PROPERTIES.get(ctx.getSide()), TriState.DEFAULT);
            state = state.with(FACING_PROPERTIES.get(ctx.getSide().getOpposite()), TriState.DEFAULT);
        } else if (i == 1) {
            state = state.with(FACING_PROPERTIES.get(lastDir.getOpposite()), TriState.DEFAULT);
        }

        return waterLog(ctx, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(LOCKED, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }


    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        if (state.get(LOCKED)) {
            return state;
        }

        var i = 0;

        Direction lastKnown = null;

        state = state.with(FACING_PROPERTIES.get(direction), TriState.of(canConnectTo(world, neighborPos, neighborState, direction.getOpposite())));
        for (var dir : Direction.values()) {
            if (state.get(FACING_PROPERTIES.get(dir)).orElse(false)) {
                i++;
                lastKnown = dir;
            } else {
                state = state.with(FACING_PROPERTIES.get(dir), TriState.FALSE);
            }
        }

        if (i == 0) {
            state = state.with(FACING_PROPERTIES.get(direction), TriState.DEFAULT);
            state = state.with(FACING_PROPERTIES.get(direction.getOpposite()), TriState.DEFAULT);
        } else if (i == 1) {
            state = state.with(FACING_PROPERTIES.get(lastKnown.getOpposite()), TriState.DEFAULT);
        }

        return state;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof WallBlock wallBlock) {
            var convert = PipeInWallBlock.MAP.get(wallBlock);
            if (convert != null) {
                stack.decrementUnlessCreative(1, player);
                var convertState = Objects.requireNonNull(convert.getPlacementState(new ItemPlacementContext(world, player, hand, stack, hit)));
                var container = world.getBlockEntity(pos) instanceof PipeBlockEntity be ? be.container : null;
                world.setBlockState(pos, convertState);
                if (container != null && world.getBlockEntity(pos) instanceof PipeBlockEntity be) {
                    container.forEach(be.container::set);
                }
                return ActionResult.SUCCESS_SERVER;
            }
        }

        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    protected boolean canConnectTo(WorldView world, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        return neighborState.getBlock() instanceof PipeConnectable connectable && connectable.canPipeConnect(world, neighborPos, neighborState, direction);
    }

    public EnumSet<Direction> getFlowDirections(BlockState state) {
        var list = new ArrayList<Direction>(6);

        for (var dir : Direction.values()) {
            if (state.get(FACING_PROPERTIES.get(dir)).orElse(true)) {
                list.add(dir);
            }
        }

        return list.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(list);
    }

    @Override
    public boolean canPipeConnect(WorldView world, BlockPos pos, BlockState state, Direction dir) {
        return !state.get(LOCKED) || super.canPipeConnect(world, pos, state, dir);
    }

    @Override
    public boolean checkModelDirection(BlockState state, Direction direction) {
        return state.get(FACING_PROPERTIES.get(direction)).orElse(true);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.rotate(state, NORTH, SOUTH, EAST, WEST, rotation);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.mirror(state, NORTH, SOUTH, EAST, WEST, mirror);
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return WRENCH_ACTIONS;
    }
}
