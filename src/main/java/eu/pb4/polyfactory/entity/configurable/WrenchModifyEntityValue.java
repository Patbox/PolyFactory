package eu.pb4.polyfactory.entity.configurable;

import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.function.BiFunction;

public interface WrenchModifyEntityValue<T, E extends Entity> {
    static <T, E extends Entity> WrenchModifyEntityValue<T, E> simple(BiFunction<T, Boolean, T> transformer) {
        return (value, next, player, entity, targetPos) -> transformer.apply(value, next);
    }

    static <T, E extends Entity> WrenchModifyEntityValue<T, E> iterate(List<T> values) {
        return (val, next, player, entity, targetPos) -> next ? Util.findNextInIterable(values, val) : Util.findPreviousInIterable(values, val);
    }

    T modifyValue(T value, boolean next, Player player, E entity, Vec3 targetPos);
}
