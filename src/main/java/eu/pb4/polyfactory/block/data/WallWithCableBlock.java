package eu.pb4.polyfactory.block.data;

import com.mojang.serialization.MapCodec;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.other.StatePropertiesCodecPatcher;
import eu.pb4.polyfactory.block.other.XInWallBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.mixin.util.WallBlockAccessor;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;

public class WallWithCableBlock extends AbstracterCableBlock implements BlockStateNameProvider, StatePropertiesCodecPatcher, XInWallBlock {
    public static final Map<Block, WallWithCableBlock> MAP = new IdentityHashMap<>();

    public static final BooleanProperty UP_WALL = BooleanProperty.create("up_wall");
    public static final EnumProperty<Side> EAST_SHAPE = EnumProperty.create("east", Side.class);
    public static final EnumProperty<Side> NORTH_SHAPE = EnumProperty.create("north", Side.class);
    public static final EnumProperty<Side> SOUTH_SHAPE = EnumProperty.create("south", Side.class);
    public static final EnumProperty<Side> WEST_SHAPE = EnumProperty.create("west", Side.class);

    @SuppressWarnings("rawtypes")
    private static final List<Pair> SELF_TO_BACKING = List.of(
            new Pair<>(EAST_SHAPE, WallBlock.EAST, x -> x == Side.WALL ? WallSide.TALL : WallSide.NONE, x -> x == WallSide.NONE ? Side.NONE : Side.WALL),
            new Pair<>(WEST_SHAPE, WallBlock.WEST, x -> x == Side.WALL ? WallSide.TALL : WallSide.NONE, x -> x == WallSide.NONE ? Side.NONE : Side.WALL),
            new Pair<>(SOUTH_SHAPE, WallBlock.SOUTH, x -> x == Side.WALL ? WallSide.TALL : WallSide.NONE, x -> x == WallSide.NONE ? Side.NONE : Side.WALL),
            new Pair<>(NORTH_SHAPE, WallBlock.NORTH, x -> x == Side.WALL ? WallSide.TALL : WallSide.NONE, x -> x == WallSide.NONE ? Side.NONE : Side.WALL),
            new Pair<>(UP_WALL, WallBlock.UP, Function.identity(), Function.identity())
    );
    private final WallBlock backing;

    @Nullable
    public static BlockState fromWall(BlockState source) {
        var block = MAP.get(source.getBlock());
        if (block == null) {
            return null;
        }

        var state = block.defaultBlockState();
        for (var prop : SELF_TO_BACKING) {
            //noinspection unchecked,rawtypes
            state = state.setValue(prop.self, (Comparable) prop.backingToSelf.apply(source.getValue(prop.backing)));
        }
        return state;
    }

    public WallWithCableBlock(Properties settings, WallBlock wallBlock) {
        super(settings);
        this.backing = wallBlock;
        this.registerDefaultState(this.defaultBlockState());
        MAP.put(wallBlock, this);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return this.backing.defaultBlockState().getCloneItemStack(world, pos, includeData);
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
        return direction.getAxis() != Direction.Axis.Y && state.getValue(getProperty(direction)) == Side.CABLE;
    }

    public boolean setColor(BlockState state, Level world, BlockPos pos, int color) {
        color = FactoryItems.CABLE.downSampleColor(color);
        if (world.getBlockEntity(pos) instanceof ColorProvider provider && provider.getColor() != color) {
            provider.setColor(color);
            var newState = state;
            for (var dir : Direction.Plane.HORIZONTAL) {
                var newPos = pos.relative(dir);
                var block = world.getBlockState(newPos);
                var prop = getProperty(dir);
                newState = newState.setValue(prop, newState.getValue(prop).cable(canConnectTo(world, provider.getColor(), newPos, block, dir.getOpposite())));
            }
            if (state != newState) {
                world.setBlockAndUpdate(pos, newState);
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
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        var hasReceivers = false;
        var hasProviders = false;
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            var newState = state;
            for (var dir : Direction.values()) {
                var newPos = pos.relative(dir);
                var block = world.getBlockState(newPos);
                if (canConnectTo(world, be.getColor(), newPos, block, dir.getOpposite())) {
                    if (dir.getAxis() != Direction.Axis.Y) {
                        newState = newState.setValue(getProperty(dir), newState.getValue(getProperty(dir)).cable(true));
                    }
                    if (placer instanceof ServerPlayer serverPlayer && (!hasReceivers || !hasProviders)) {
                        var net = NetworkComponent.Data.getLogic(serverPlayer.level(), newPos);
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
                world.setBlockAndUpdate(pos, newState);
            }
        }
        if (hasReceivers && hasProviders) {
            TriggerCriterion.trigger((ServerPlayer) placer, FactoryTriggers.CABLE_CONNECT);
        }

    }


    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var x = this.backing.getStateForPlacement(ctx);
        var state = this.defaultBlockState();
        for (var prop : SELF_TO_BACKING) {
            assert x != null;
            //noinspection unchecked,rawtypes
            state = state.setValue(prop.self, (Comparable) prop.backingToSelf.apply(x.getValue(prop.backing)));
        }

        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        var x = ((WallBlockAccessor) this.backing).callUpdateShape(this.getPolymerBlockState(state, null), world, tickView, pos, direction, neighborPos, neighborState, random);
        for (var prop : SELF_TO_BACKING) {
            assert x != null;
            //noinspection unchecked,rawtypes
            var val = (Comparable) prop.backingToSelf.apply(x.getValue(prop.backing));
            //noinspection unchecked
            state = state.setValue(prop.self, val);
        }

        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            for (var dir : Direction.Plane.HORIZONTAL) {
                var prop = getProperty(dir);
                var nextPos = pos.relative(dir);
                var val = canConnectTo(world, be.getColor(), nextPos, world.getBlockState(nextPos), dir.getOpposite());
                state = state.setValue(prop, state.getValue(prop).cable(val));
            }
        }


        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EAST_SHAPE, NORTH_SHAPE, SOUTH_SHAPE, WEST_SHAPE, UP_WALL);
    }

    protected boolean isTransparent(BlockState state, BlockGetter world, BlockPos pos) {
        return true;
    }

    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        return this.getPolymerBlockState(state, null).getDestroyProgress(player, world, pos);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var list = new ArrayList<ItemStack>();
        list.addAll(this.getPolymerBlockState(state, null).getDrops(builder));
        list.addAll(FactoryBlocks.CABLE.defaultBlockState().getDrops(builder));
        return list;
    }


    @Override
    public BlockState convertToBacking(BlockState state) {
        var backing = this.backing.defaultBlockState();
        for (var x : SELF_TO_BACKING) {
            //noinspection unchecked,rawtypes
            backing = backing.setValue(x.backing, (Comparable) x.selfToBacking.apply(state.getValue(x.self)));
        }
        return backing;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        var converted = this.convertToBacking(initialBlockState);
        var elementHolder = BlockWithElementHolder.get(converted) instanceof BlockWithElementHolder holder ? holder.createElementHolder(world, pos, converted) : null;
        return new Model(initialBlockState, elementHolder, this::convertToBacking);
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("block.polyfactory.wall_with_cable", this.backing.getName());
    }

    @Override
    public Component getName(ServerLevel world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof ColorProvider be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Component.translatable("block.polyfactory.wall_with_cable.colored.full",
                        this.backing.getName(),
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Component.translatable("block.polyfactory.wall_with_cable.colored", this.backing.getName(), ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }

    @Override
    public WallBlock backing() {
        return this.backing;
    }

    @Override
    public MapCodec<BlockState> modifyPropertiesCodec(MapCodec<BlockState> codec) {
        return StatePropertiesCodecPatcher.modifier(codec, (state, ops, input) -> {

            for (var dir : Direction.Plane.HORIZONTAL) {
                var wall = input.get(dir.getSerializedName() + "_wall");
                var cable = input.get(dir.getSerializedName());
                if (wall != null && cable != null) {
                    state = state.setValue(getProperty(dir), Side.NONE.wall(ops.getStringValue(wall).getOrThrow().equals("true"))
                            .cable(ops.getStringValue(cable).getOrThrow().equals("true")));
                }
            }

            return state;
        });
    }


    public static final class Model extends AbstracterCableBlock.BaseCableModel {
        @Nullable
        private final ProxyAttachement proxied;

        public Model(BlockState state, @Nullable ElementHolder elementHolder, Function<BlockState, BlockState> convertToBacking) {
            super(state);
            if (elementHolder != null) {
                this.proxied = new ProxyAttachement(this, elementHolder, () -> convertToBacking.apply(blockState()));
                elementHolder.setAttachment(this.proxied);
                this.addElement(proxied);
            } else {
                proxied = null;
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            super.notifyUpdate(updateType);
            if (this.proxied != null) {
                this.proxied.holder().notifyUpdate(updateType);
            }
        }

        @Override
        public ItemStack getModel(BlockState state, BiPredicate<BlockState, Direction> directionPredicate) {
            return FactoryModels.COLORED_WALL_CABLE.get(state, directionPredicate).copy();
        }
    }

    private record Pair<A extends Comparable<A>, B extends Comparable<B>>(Property<A> self, Property<B> backing, Function<A, B> selfToBacking, Function<B, A> backingToSelf) { }

    public enum Side implements StringRepresentable {
        NONE("none"),
        WALL("wall"),
        CABLE("cable");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
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
