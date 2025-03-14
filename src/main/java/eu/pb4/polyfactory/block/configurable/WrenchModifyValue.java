package eu.pb4.polyfactory.block.configurable;

import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.function.BiFunction;

public interface WrenchModifyValue<T> {
    @SuppressWarnings({"unchecked", "rawtypes", "RedundantCast"})
    static <T extends Comparable<T>> WrenchModifyValue<T> ofProperty(Property<T> property) {
        return (value, next, player, world, pos, side, state) -> {
            var elements = property.getValues();
            return !next ? Util.previous(elements, value) : Util.next(elements, value);
        };
    }

    static <T> WrenchModifyValue<T> simple(BiFunction<T, Boolean, T> transformer) {
        return (value, next, player, world, pos, side, state) -> transformer.apply(value, next);
    }

    static WrenchModifyValue<Direction> ofDirection(EnumProperty<Direction> property) {
        var reordered = new ArrayList<Direction>();
        for (var x : FactoryUtil.REORDERED_DIRECTIONS) {
            if (property.getValues().contains(x)) {
                reordered.add(x);
            }
        }
        return (dir, next, player, world, pos, side, state) -> reordered.get((reordered.size() + reordered.indexOf(dir) + (next ? 1 : -1)) % reordered.size());
    }

    static WrenchModifyValue<Direction> ofAltDirection(EnumProperty<Direction> property) {
        return (dir, next, player, world, pos, side, state) -> {
            var val = next ? side : side.getOpposite();
            return property.getValues().contains(val) ? val : dir;
        };
    }

    T modifyValue(T value, boolean next, PlayerEntity player, World world, BlockPos pos, Direction side, BlockState state);
}
