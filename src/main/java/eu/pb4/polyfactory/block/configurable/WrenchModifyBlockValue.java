package eu.pb4.polyfactory.block.configurable;

import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.Orientation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.function.BiFunction;

public interface WrenchModifyBlockValue<T> {
    @SuppressWarnings({"unchecked", "rawtypes", "RedundantCast"})
    static <T extends Comparable<T>> WrenchModifyBlockValue<T> ofProperty(Property<T> property) {
        return (value, next, player, world, pos, side, state) -> {
            var elements = property.getValues();
            return !next ? Util.previous(elements, value) : Util.next(elements, value);
        };
    }

    static <T> WrenchModifyBlockValue<T> simple(BiFunction<T, Boolean, T> transformer) {
        return (value, next, player, world, pos, side, state) -> transformer.apply(value, next);
    }

    static WrenchModifyBlockValue<Direction> ofDirection(EnumProperty<Direction> property) {
        var reordered = new ArrayList<Direction>();
        for (var x : FactoryUtil.REORDERED_DIRECTIONS) {
            if (property.getValues().contains(x)) {
                reordered.add(x);
            }
        }
        return (dir, next, player, world, pos, side, state) -> reordered.get((reordered.size() + reordered.indexOf(dir) + (next ? 1 : -1)) % reordered.size());
    }

    static WrenchModifyBlockValue<Direction> ofAltDirection(EnumProperty<Direction> property) {
        return (dir, next, player, world, pos, side, state) -> {
            var val = next ? side : side.getOpposite();
            return property.getValues().contains(val) ? val : dir;
        };
    }

    static WrenchModifyBlockValue<Orientation> ofAltOrientation(EnumProperty<Orientation> property) {
        return (dir, next, player, world, pos, side, state) -> {
            var val = next ? side : side.getOpposite();
            var dir2 = switch (val) {
                case DOWN -> player.getHorizontalFacing();
                case UP -> player.getHorizontalFacing().getOpposite();
                default -> Direction.UP;
            };

            var orientation = Orientation.byDirections(val, dir2);
            return property.getValues().contains(orientation) ? orientation : dir;
        };
    }

    T modifyValue(T value, boolean next, PlayerEntity player, World world, BlockPos pos, Direction side, BlockState state);
}
