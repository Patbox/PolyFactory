package eu.pb4.polyfactory.block.configurable;

import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.ArrayList;
import java.util.function.BiFunction;

public interface WrenchModifyBlockValue<T> {
    @SuppressWarnings({"unchecked", "rawtypes", "RedundantCast"})
    static <T extends Comparable<T>> WrenchModifyBlockValue<T> ofProperty(Property<T> property) {
        return (value, next, player, world, pos, side, state) -> {
            var elements = property.getPossibleValues();
            return !next ? Util.findPreviousInIterable(elements, value) : Util.findNextInIterable(elements, value);
        };
    }

    static <T> WrenchModifyBlockValue<T> simple(BiFunction<T, Boolean, T> transformer) {
        return (value, next, player, world, pos, side, state) -> transformer.apply(value, next);
    }

    static WrenchModifyBlockValue<Direction> ofDirection(EnumProperty<Direction> property) {
        var reordered = new ArrayList<Direction>();
        for (var x : FactoryUtil.REORDERED_DIRECTIONS) {
            if (property.getPossibleValues().contains(x)) {
                reordered.add(x);
            }
        }
        return (dir, next, player, world, pos, side, state) -> reordered.get((reordered.size() + reordered.indexOf(dir) + (next ? 1 : -1)) % reordered.size());
    }

    static WrenchModifyBlockValue<Direction> ofAltDirection(EnumProperty<Direction> property) {
        return (dir, next, player, world, pos, side, state) -> {
            var val = next ? side : side.getOpposite();
            return property.getPossibleValues().contains(val) ? val : dir;
        };
    }

    static WrenchModifyBlockValue<FrontAndTop> ofAltOrientation(EnumProperty<FrontAndTop> property) {
        return (dir, next, player, world, pos, side, state) -> {
            var val = next ? side : side.getOpposite();
            var dir2 = switch (val) {
                case DOWN -> player.getDirection();
                case UP -> player.getDirection().getOpposite();
                default -> Direction.UP;
            };

            var orientation = FrontAndTop.fromFrontAndTop(val, dir2);
            return property.getPossibleValues().contains(orientation) ? orientation : dir;
        };
    }

    T modifyValue(T value, boolean next, Player player, Level world, BlockPos pos, Direction side, BlockState state);
}
