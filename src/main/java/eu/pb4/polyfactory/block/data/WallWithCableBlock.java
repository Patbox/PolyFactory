package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.mixin.util.WallBlockAccessor;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public class WallWithCableBlock extends AbstractCableBlock implements PolymerBlock, BlockWithElementHolder, Waterloggable, BlockStateNameProvider {
    public static final Map<Block, WallWithCableBlock> MAP = new IdentityHashMap<>();

    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final BooleanProperty UP_WALL = BooleanProperty.of("up_wall");
    public static final EnumProperty<WallShape> EAST_WALL_SHAPE = EnumProperty.of("east_wall", WallShape .class);
    public static final EnumProperty<WallShape> NORTH_WALL_SHAPE = EnumProperty.of("north_wall", WallShape.class);
    public static final EnumProperty<WallShape> SOUTH_WALL_SHAPE = EnumProperty.of("south_wall", WallShape.class);
    public static final EnumProperty<WallShape> WEST_WALL_SHAPE = EnumProperty.of("west_wall", WallShape.class);

    @SuppressWarnings("rawtypes")
    private static final List<Pair> SELF_TO_BACKING = List.of(
            new Pair<>(WATERLOGGED, WATERLOGGED),
            new Pair<>(EAST_WALL_SHAPE, WallBlock.EAST_SHAPE),
            new Pair<>(WEST_WALL_SHAPE, WallBlock.WEST_SHAPE),
            new Pair<>(SOUTH_WALL_SHAPE, WallBlock.SOUTH_SHAPE),
            new Pair<>(NORTH_WALL_SHAPE, WallBlock.NORTH_SHAPE),
            new Pair<>(UP_WALL, WallBlock.UP)
    );
    private final WallBlock backing;

    @Nullable
    public static BlockState fromWall(BlockState source) {
        var block = MAP.get(source.getBlock());
        if (block == null) {
            return null;
        }

        var state = block.getDefaultState();
        for (var prop : SELF_TO_BACKING) {
            //noinspection unchecked
            state = state.with(prop.self, source.get(prop.backing));
        }
        return state;
    }

    public WallWithCableBlock(WallBlock wallBlock) {
        super(AbstractBlock.Settings.copy(wallBlock));
        this.backing = wallBlock;
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
        MAP.put(wallBlock, this);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return this.backing.getPickStack(world, pos, getPolymerBlockState(state));
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var x = this.backing.getPlacementState(ctx);
        var state = this.getDefaultState();
        for (var prop : SELF_TO_BACKING) {
            assert x != null;
            //noinspection unchecked
            state = state.with(prop.self, x.get(prop.backing));
        }

        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var x = ((WallBlockAccessor) this.backing).callGetStateForNeighborUpdate(this.getPolymerBlockState(state), direction, neighborState, world, pos, neighborPos);
        for (var prop : SELF_TO_BACKING) {
            assert x != null;
            //noinspection unchecked
            state = state.with(prop.self, x.get(prop.backing));
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(EAST_WALL_SHAPE, NORTH_WALL_SHAPE, SOUTH_WALL_SHAPE, WEST_WALL_SHAPE, UP_WALL, WATERLOGGED);
    }


    protected FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return !state.get(WATERLOGGED);
    }

    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        var backing = this.backing.getDefaultState();
        for (var x : SELF_TO_BACKING) {
            //noinspection unchecked
            backing = backing.with(x.backing, state.get(x.self));
        }
        return backing;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public MutableText getName() {
        return Text.translatable(this.getTranslationKey(), this.backing.getName());
    }

    @Override
    public Text getName(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof ColorProvider be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Text.translatable("block.polyfactory.wall_with_cable.colored.full",
                        this.backing.getName(),
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Text.translatable("block.polyfactory.wall_with_cable.colored", this.backing.getName(), ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }

    public WallBlock getBacking() {
        return this.backing;
    }


    public static final class Model extends BaseCableModel {
        public Model(BlockState state) {
            super(state);
        }

        @Override
        public ItemStack getModel(BlockState state, BiPredicate<BlockState, Direction> directionPredicate) {
            return FactoryModels.COLORED_WALL_CABLE.get(state, directionPredicate).copy();
        }
    }

    private record Pair<T extends Comparable<T>>(Property<T> self, Property<T> backing) { }
}
