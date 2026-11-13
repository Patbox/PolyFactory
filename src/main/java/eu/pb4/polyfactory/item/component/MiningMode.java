package eu.pb4.polyfactory.item.component;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.util.BlockTraversal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Consumer;

public enum MiningMode implements StringRepresentable, TooltipProvider {
    SINGLE {
        @Override
        public Iterable<BlockPos> positions(Player player, BlockGetter level, BlockPos pos, BlockState state, Direction side) {
            return List.of(pos);
        }
    },
    AREA_2X2X1 {
        @Override
        public Iterable<BlockPos> positions(Player player, BlockGetter level, BlockPos pos, BlockState state, Direction side) {
            var from = player.getEyePosition();
            var to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot()).scale(player.blockInteractionRange()));
            var result = level.clip(new ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

            var axis1 = switch (side.getAxis()) {
                case X, Z -> Direction.Axis.Y;
                case Y -> Direction.Axis.X;
            };

            var axis2 = switch (side.getAxis()) {
                case X, Y -> Direction.Axis.Z;
                case Z -> Direction.Axis.X;
            };

            var loc = result.getLocation().relative(side, -0.05);

            return BlockPos.betweenClosed(
                    BlockPos.containing(loc.relative(axis1.getPositive(), 0.5).relative(axis2.getPositive(), 0.5)),
                    BlockPos.containing(loc.relative(axis1.getNegative(), 0.5).relative(axis2.getNegative(), 0.5)));
        }
    },
    AREA_3X3X1 {
        @Override
        public Iterable<BlockPos> positions(Player player, BlockGetter level, BlockPos pos, BlockState state, Direction side) {
            var axis1 = switch (side.getAxis()) {
                case X, Z -> Direction.Axis.Y;
                case Y -> Direction.Axis.X;
            };

            var axis2 = switch (side.getAxis()) {
                case X, Y -> Direction.Axis.Z;
                case Z -> Direction.Axis.X;
            };

            return BlockPos.betweenClosed(pos.relative(axis1.getPositive()).relative(axis2.getPositive()), pos.relative(axis1.getNegative()).relative(axis2.getNegative()));
        }
    },
    VEINMINER_9X {
        private final BlockTraversal traversal = BlockTraversal.create()
                .connectivity(BlockTraversal.Connectivity.TWENTY_SIX)
                .order(BlockTraversal.Order.BREADTH_FIRST);
        @Override
        public Iterable<BlockPos> positions(Player player, BlockGetter level, BlockPos pos, BlockState state, Direction side) {
            var list = new ArrayList<BlockPos>();

            traversal.accept(pos, (target, fromPos, depth) -> {
                if (level.getBlockState(target).is(state.getBlock()) && depth < 10) {
                    list.add(target.immutable());
                    return BlockTraversal.Result.CONTINUE;
                }
                return BlockTraversal.Result.TERMINATE;
            });

            Collections.shuffle(list);
            list.sort(Comparator.comparingDouble(x -> x.distSqr(pos)));

            return list.subList(0, Math.min(list.size(), 9));
        }
    };
    public static final Codec<MiningMode> CODEC = StringRepresentable.fromEnum(MiningMode::values);

    public static boolean skipMiningFor(Player player, BlockGetter level, BlockPos pos, BlockState state) {
        return state.isAir() || state.getDestroySpeed(level, pos) < 0 || !player.getMainHandItem().canDestroyBlock(state, (Level) level, pos, player);
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public abstract Iterable<BlockPos> positions(Player player, BlockGetter level, BlockPos pos, BlockState state, Direction side);

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(this.tooltip().withColor(TextColor.GRAY));
    }

    public MutableComponent tooltip() {
        return Component.translatable("tooltip.polyfactory.mining_mode", Component.translatable("mining_mode.polyfactory." + this.getSerializedName()));
    }
}
