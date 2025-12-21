package eu.pb4.polyfactory.entity.configurable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface EntityValueFormatter<T, E extends Entity> {
    static <T, E extends Entity> EntityValueFormatter<T, E> getDefault() {
        return (val, ent, pos) -> Component.literal(String.valueOf(val));
    }

    Component getDisplayValue(T value, E entity, Vec3 pos);

    default <A extends Entity> EntityValueFormatter<T, A> cast() {
        return (EntityValueFormatter<T, A>) this;
    }

    static <T, E extends Entity> EntityValueFormatter<T, E> text(TextFunc<T> format) {
        return ((value, ent, pos) -> format.apply(value));
    }

    static <T, E extends Entity> EntityValueFormatter<T, E> str(StringFunc<T> format) {
        return ((value, ent, pos) -> Component.literal(format.apply(value)));
    }

    interface TextFunc<T> {
        Component apply(T value);
    }

    interface StringFunc<T> {
        String apply(T value);
    }
}
