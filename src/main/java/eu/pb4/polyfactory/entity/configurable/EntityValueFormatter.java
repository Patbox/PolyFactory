package eu.pb4.polyfactory.entity.configurable;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface EntityValueFormatter<T, E extends Entity> {
    static <T, E extends Entity> EntityValueFormatter<T, E> getDefault() {
        return (val, ent, pos) -> Text.literal(String.valueOf(val));
    }

    Text getDisplayValue(T value, E entity, Vec3d pos);

    default <A extends Entity> EntityValueFormatter<T, A> cast() {
        return (EntityValueFormatter<T, A>) this;
    }

    static <T, E extends Entity> EntityValueFormatter<T, E> text(TextFunc<T> format) {
        return ((value, ent, pos) -> format.apply(value));
    }

    static <T, E extends Entity> EntityValueFormatter<T, E> str(StringFunc<T> format) {
        return ((value, ent, pos) -> Text.literal(format.apply(value)));
    }

    interface TextFunc<T> {
        Text apply(T value);
    }

    interface StringFunc<T> {
        String apply(T value);
    }
}
