package eu.pb4.polyfactory.block.fluids.transport;

import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.block.property.LazyEnumProperty;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipeBlock extends PipeBaseBlock implements ConfigurableBlock {

    public static final BooleanProperty LOCKED = FactoryProperties.LOCKED;
    public static final LazyEnumProperty<TriState> NORTH = FactoryProperties.TRI_STATE_NORTH;
    public static final LazyEnumProperty<TriState> SOUTH = FactoryProperties.TRI_STATE_SOUTH;

    public static final LazyEnumProperty<TriState> EAST = FactoryProperties.TRI_STATE_EAST;
    public static final LazyEnumProperty<TriState> WEST = FactoryProperties.TRI_STATE_WEST;
    public static final LazyEnumProperty<TriState> UP = FactoryProperties.TRI_STATE_UP;
    public static final LazyEnumProperty<TriState> DOWN = FactoryProperties.TRI_STATE_DOWN;
    private static final List<BlockConfig<?>> WRENCH_ACTIONS = List.of(BlockConfig.of("locked", LOCKED));
    public static final Map<Direction, LazyEnumProperty<TriState>> FACING_PROPERTIES = FactoryProperties.TRI_STATE_DIRECTIONS;

    public PipeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(LOCKED, false).setValue(NORTH, TriState.FALSE).setValue(SOUTH, TriState.FALSE)
                .setValue(EAST, TriState.FALSE).setValue(WEST, TriState.FALSE).setValue(UP, TriState.FALSE).setValue(DOWN, TriState.FALSE));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var state = this.defaultBlockState();
        int i = 0;
        Direction lastDir = null;
        for (var direction : Direction.values()) {
            var neighborPos = ctx.getClickedPos().relative(direction);
            if (canConnectTo(ctx.getLevel(), neighborPos, ctx.getLevel().getBlockState(neighborPos), direction.getOpposite())) {
                state = state.setValue(FACING_PROPERTIES.get(direction), TriState.TRUE);
                i++;
                lastDir = direction;
            }
        }
        if (i == 0) {
            state = state.setValue(FACING_PROPERTIES.get(ctx.getClickedFace()), TriState.DEFAULT);
            state = state.setValue(FACING_PROPERTIES.get(ctx.getClickedFace().getOpposite()), TriState.DEFAULT);
        } else if (i == 1) {
            state = state.setValue(FACING_PROPERTIES.get(lastDir.getOpposite()), TriState.DEFAULT);
        }

        return waterLog(ctx, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LOCKED, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }


    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        if (state.getValue(LOCKED)) {
            return state;
        }

        var i = 0;

        Direction lastKnown = null;

        state = state.setValue(FACING_PROPERTIES.get(direction), TriState.of(canConnectTo(world, neighborPos, neighborState, direction.getOpposite())));
        for (var dir : Direction.values()) {
            if (state.getValue(FACING_PROPERTIES.get(dir)).orElse(false)) {
                i++;
                lastKnown = dir;
            } else {
                state = state.setValue(FACING_PROPERTIES.get(dir), TriState.FALSE);
            }
        }

        if (i == 0) {
            state = state.setValue(FACING_PROPERTIES.get(direction), TriState.DEFAULT);
            state = state.setValue(FACING_PROPERTIES.get(direction.getOpposite()), TriState.DEFAULT);
        } else if (i == 1) {
            state = state.setValue(FACING_PROPERTIES.get(lastKnown.getOpposite()), TriState.DEFAULT);
        }

        return state;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof WallBlock wallBlock) {
            var convert = PipeInWallBlock.MAP.get(wallBlock);
            if (convert != null) {
                stack.consume(1, player);
                var convertState = Objects.requireNonNull(convert.getStateForPlacement(new BlockPlaceContext(world, player, hand, stack, hit)));
                var container = world.getBlockEntity(pos) instanceof PipeBlockEntity be ? be.container : null;
                world.setBlockAndUpdate(pos, convertState);
                if (container != null && world.getBlockEntity(pos) instanceof PipeBlockEntity be) {
                    container.forEach(be.container::set);
                }
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    protected boolean canConnectTo(LevelReader world, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        return (neighborState.getBlock() instanceof PipeConnectable connectable && connectable.canPipeConnect(world, neighborPos, neighborState, direction))
                || neighborState.is(FactoryBlockTags.PIPE_CONNECTABLE);
    }

    public EnumSet<Direction> getFlowDirections(BlockState state) {
        var list = new ArrayList<Direction>(6);

        for (var dir : Direction.values()) {
            if (state.getValue(FACING_PROPERTIES.get(dir)).orElse(true)) {
                list.add(dir);
            }
        }

        return list.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(list);
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return !state.getValue(LOCKED) || super.canPipeConnect(world, pos, state, dir);
    }

    @Override
    public boolean checkModelDirection(BlockState state, Direction direction) {
        return state.getValue(FACING_PROPERTIES.get(direction)).orElse(true);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.rotate(state, NORTH, SOUTH, EAST, WEST, rotation);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.mirror(state, NORTH, SOUTH, EAST, WEST, mirror);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }
}
