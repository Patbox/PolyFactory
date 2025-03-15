package eu.pb4.polyfactory.block.data;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.other.StatePropertiesCodecPatcher;
import eu.pb4.polyfactory.item.FactoryItems;
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
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class WallWithCableBlock extends AbstracterCableBlock implements PolymerBlock, BlockWithElementHolder, BlockStateNameProvider, StatePropertiesCodecPatcher {
    public static final Map<Block, WallWithCableBlock> MAP = new IdentityHashMap<>();

    public static final BooleanProperty UP_WALL = BooleanProperty.of("up_wall");
    public static final EnumProperty<Side> EAST_SHAPE = EnumProperty.of("east", Side.class);
    public static final EnumProperty<Side> NORTH_SHAPE = EnumProperty.of("north", Side.class);
    public static final EnumProperty<Side> SOUTH_SHAPE = EnumProperty.of("south", Side.class);
    public static final EnumProperty<Side> WEST_SHAPE = EnumProperty.of("west", Side.class);

    @SuppressWarnings("rawtypes")
    private static final List<Pair> SELF_TO_BACKING = List.of(
            new Pair<>(EAST_SHAPE, WallBlock.EAST_SHAPE, x -> x == Side.WALL ? WallShape.TALL : WallShape.NONE, x -> x == WallShape.NONE ? Side.NONE : Side.WALL),
            new Pair<>(WEST_SHAPE, WallBlock.WEST_SHAPE, x -> x == Side.WALL ? WallShape.TALL : WallShape.NONE, x -> x == WallShape.NONE ? Side.NONE : Side.WALL),
            new Pair<>(SOUTH_SHAPE, WallBlock.SOUTH_SHAPE, x -> x == Side.WALL ? WallShape.TALL : WallShape.NONE, x -> x == WallShape.NONE ? Side.NONE : Side.WALL),
            new Pair<>(NORTH_SHAPE, WallBlock.NORTH_SHAPE, x -> x == Side.WALL ? WallShape.TALL : WallShape.NONE, x -> x == WallShape.NONE ? Side.NONE : Side.WALL),
            new Pair<>(UP_WALL, WallBlock.UP, Function.identity(), Function.identity())
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
            //noinspection unchecked,rawtypes
            state = state.with(prop.self, (Comparable) prop.backingToSelf.apply(source.get(prop.backing)));
        }
        return state;
    }

    public WallWithCableBlock(WallBlock wallBlock) {
        super(AbstractBlock.Settings.copy(wallBlock));
        this.backing = wallBlock;
        this.setDefaultState(this.getDefaultState());
        MAP.put(wallBlock, this);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return this.backing.getPickStack(world, pos, getPolymerBlockState(state));
    }

    @Override
    public EnumSet<Direction> getDirections(BlockState state) {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    protected boolean isDirectionBlocked(BlockState state, Direction direction) {
        return false;
    }

    @Override
    protected boolean checkModelDirection(BlockState state, Direction direction) {
        return direction.getAxis() != Direction.Axis.Y && state.get(getProperty(direction)) == Side.CABLE;
    }

    public boolean setColor(BlockState state, World world, BlockPos pos, int color) {
        color = FactoryItems.CABLE.downSampleColor(color);
        if (world.getBlockEntity(pos) instanceof ColorProvider provider && provider.getColor() != color) {
            provider.setColor(color);
            var newState = state;
            for (var dir : Direction.Type.HORIZONTAL) {
                var newPos = pos.offset(dir);
                var block = world.getBlockState(newPos);
                var prop = getProperty(dir);
                newState = newState.with(prop, newState.get(prop).cable(canConnectTo(world, provider.getColor(), newPos, block, dir.getOpposite())));
            }
            if (state != newState) {
                world.setBlockState(pos, newState);
            }
            return true;
        }

        return false;
    }

    private Property<Side> getProperty(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> null;
        };
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        var hasReceivers = false;
        var hasProviders = false;
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            var newState = state;
            for (var dir : Direction.values()) {
                var newPos = pos.offset(dir);
                var block = world.getBlockState(newPos);
                if (canConnectTo(world, be.getColor(), newPos, block, dir.getOpposite())) {
                    if (dir.getAxis() != Direction.Axis.Y) {
                        newState = newState.with(getProperty(dir), newState.get(getProperty(dir)).cable(true));
                    }
                    if (placer instanceof ServerPlayerEntity serverPlayer && (!hasReceivers || !hasProviders)) {
                        var net = NetworkComponent.Data.getLogic(serverPlayer.getServerWorld(), newPos);
                        if (net.hasReceivers()) {
                            hasReceivers = true;
                        }

                        if (net.hasProviders()) {
                            hasProviders = true;
                        }
                    }
                }
            }

            if (state != newState) {
                world.setBlockState(pos, newState);
            }
        }
        if (hasReceivers && hasProviders) {
            TriggerCriterion.trigger((ServerPlayerEntity) placer, FactoryTriggers.CABLE_CONNECT);
        }

    }


    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var x = this.backing.getPlacementState(ctx);
        var state = this.getDefaultState();
        for (var prop : SELF_TO_BACKING) {
            assert x != null;
            //noinspection unchecked,rawtypes
            state = state.with(prop.self, (Comparable) prop.backingToSelf.apply(x.get(prop.backing)));
        }

        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var x = ((WallBlockAccessor) this.backing).callGetStateForNeighborUpdate(this.getPolymerBlockState(state), direction, neighborState, world, pos, neighborPos);
        for (var prop : SELF_TO_BACKING) {
            assert x != null;
            //noinspection unchecked,rawtypes
            var val = (Comparable) prop.backingToSelf.apply(x.get(prop.backing));
            //noinspection unchecked
            state = state.with(prop.self, val);
        }

        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            for (var dir : Direction.Type.HORIZONTAL) {
                var prop = getProperty(dir);
                var nextPos = pos.offset(dir);
                var val = canConnectTo(world, be.getColor(), nextPos, world.getBlockState(nextPos), dir.getOpposite());
                state = state.with(prop, state.get(prop).cable(val));
            }
        }


        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(EAST_SHAPE, NORTH_SHAPE, SOUTH_SHAPE, WEST_SHAPE, UP_WALL);
    }

    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        var backing = this.backing.getDefaultState();
        for (var x : SELF_TO_BACKING) {
            //noinspection unchecked,rawtypes
            backing = backing.with(x.backing, (Comparable) x.selfToBacking.apply(state.get(x.self)));
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
        return Text.translatable("block.polyfactory.wall_with_cable", this.backing.getName());
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

    @Override
    public MapCodec<BlockState> modifyPropertiesCodec(MapCodec<BlockState> codec) {
        return StatePropertiesCodecPatcher.modifier(codec, (state, ops, input) -> {

            for (var dir : Direction.Type.HORIZONTAL) {
                var wall = input.get(dir.asString() + "_wall");
                var cable = input.get(dir.asString());
                if (wall != null && cable != null) {
                    state = state.with(getProperty(dir), Side.NONE.wall(ops.getStringValue(wall).getOrThrow().equals("true"))
                            .cable(ops.getStringValue(cable).getOrThrow().equals("true")));
                }
            }

            return state;
        });
    }


    public static final class Model extends AbstracterCableBlock.BaseCableModel {
        public Model(BlockState state) {
            super(state);
        }

        //@Override
        public ItemStack getModel(BlockState state, BiPredicate<BlockState, Direction> directionPredicate) {
            return FactoryModels.COLORED_WALL_CABLE.get(state, directionPredicate).copy();
        }
    }

    private record Pair<A extends Comparable<A>, B extends Comparable<B>>(Property<A> self, Property<B> backing, Function<A, B> selfToBacking, Function<B, A> backingToSelf) { }

    public enum Side implements StringIdentifiable {
        NONE("none"),
        WALL("wall"),
        CABLE("cable");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }

        public Side cable(boolean val) {
            return switch (this) {
                case NONE, CABLE -> val ? CABLE : NONE;
                case WALL -> WALL;
            };
        }

        public Side wall(boolean val) {
            return switch (this) {
                case NONE, CABLE -> val ? WALL : this;
                case WALL -> val ? WALL : NONE;
            };
        }
    }

}
