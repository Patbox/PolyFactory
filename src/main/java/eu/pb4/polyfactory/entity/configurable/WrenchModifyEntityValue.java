package eu.pb4.polyfactory.entity.configurable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.BiFunction;

public interface WrenchModifyEntityValue<T, E extends Entity> {
    static <T, E extends Entity> WrenchModifyEntityValue<T, E> simple(BiFunction<T, Boolean, T> transformer) {
        return (value, next, player, entity, targetPos) -> transformer.apply(value, next);
    }

    static <T, E extends Entity> WrenchModifyEntityValue<T, E> iterate(List<T> values) {
        return (val, next, player, entity, targetPos) -> next ? Util.next(values, val) : Util.previous(values, val);
    }

    T modifyValue(T value, boolean next, PlayerEntity player, E entity, Vec3d targetPos);
}
