package eu.pb4.polyfactory.block.fluids.transport;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.data.WallWithCableBlock;
import eu.pb4.polyfactory.block.other.ProxyAttachement;
import eu.pb4.polyfactory.block.other.TagRedirector;
import eu.pb4.polyfactory.block.other.XInWallBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.storage.loot.LootParams;

public class PipeInWallBlock extends PipeBaseBlock implements ConfigurableBlock, XInWallBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final List<BlockConfig<?>> WRENCH_ACTIONS = List.of(BlockConfig.HORIZONTAL_AXIS);
    public static final IdentityHashMap<Block, PipeInWallBlock> MAP = new IdentityHashMap<>();
    private final WallBlock backing;

    public PipeInWallBlock(Properties settings, WallBlock wallBlock) {
        super(settings);
        this.backing = wallBlock;
        MAP.put(wallBlock, this);
    }

    public static BlockState fromWall(BlockState state, UseOnContext context) {
        var base = MAP.get(state.getBlock());
        if (base == null) {
            return null;
        }
        var axis = context.getClickedFace().getAxis() == Direction.Axis.Y ? context.getHorizontalDirection().getAxis() : context.getClickedFace().getAxis();
        return base.defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED)).setValue(AXIS, axis);
    }

    public MutableComponent getName() {
        return Component.translatable("block.polyfactory.wall_with_pipe", this.backing.getName());
    }

    @Override
    public BlockState convertToBacking(BlockState state) {
        var a = state.getValue(AXIS) == Direction.Axis.X ? WallBlock.NORTH : WallBlock.WEST;
        var b = state.getValue(AXIS) == Direction.Axis.X ? WallBlock.SOUTH : WallBlock.EAST;

        return this.backing.defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED)).setValue(WallBlock.UP, false).setValue(a, WallSide.TALL).setValue(b, WallSide.TALL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var state = this.defaultBlockState();
        var axis = ctx.getClickedFace().getAxis() == Direction.Axis.Y ? ctx.getHorizontalDirection().getAxis() : ctx.getClickedFace().getAxis();

        return waterLog(ctx, state.setValue(AXIS, axis));
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        return this.getPolymerBlockState(state, null).getDestroyProgress(player, world, pos);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var list = new ArrayList<ItemStack>();
        list.addAll(this.getPolymerBlockState(state, null).getDrops(builder));
        list.addAll(FactoryBlocks.PIPE.defaultBlockState().getDrops(builder));
        return list;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    public EnumSet<Direction> getFlowDirections(BlockState state) {
        var axis = state.getValue(AXIS);
        return EnumSet.of(axis.getPositive(), axis.getNegative());
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
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }

    public WallBlock backing() {
        return this.backing;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return this.convertToBacking(state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        var converted = this.convertToBacking(initialBlockState);
        var elementHolder = BlockWithElementHolder.get(converted) instanceof BlockWithElementHolder holder ? holder.createElementHolder(world, pos, converted) : null;
        return new Model(initialBlockState, elementHolder, this::convertToBacking, world);
    }

    public static class Model extends PipeModel {
        @Nullable
        private final ProxyAttachement proxied;

        public Model(BlockState state, @Nullable ElementHolder elementHolder, Function<BlockState, BlockState> convertToBacking, ServerLevel level) {
            super(state);
            if (elementHolder != null) {
                this.proxied = new ProxyAttachement(elementHolder, () -> this.currentPos, () -> convertToBacking.apply(blockStateOr(state)), level);
            } else {
                proxied = null;
            }
        }

        private BlockState blockStateOr(BlockState state) {
            if (this.inWorld()) {
                return this.blockState();
            } else {
                return state;
            }
        }

        @Override
        public void setAttachment(@Nullable HolderAttachment attachment) {
            if (this.proxied != null) {
                if (attachment == null) {
                    this.removeElement(this.proxied);
                    this.proxied.holder().setAttachment(null);
                } else {
                    this.proxied.holder().setAttachment(this.proxied);
                    this.addElement(proxied);
                }
            }
            super.setAttachment(attachment);
        }


        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            super.notifyUpdate(updateType);
            if (this.proxied != null) {
                this.proxied.holder().notifyUpdate(updateType);
            }
        }
    }
}
