package eu.pb4.polyfactory.entity.configurable;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface EntityConfigValue<T, E extends Entity> {

    static <T, E extends Entity> EntityConfigValue<T, E> of(Function<E, T> getter, BiConsumer<E, T> setter) {
        return new EntityConfigValue<T, E>() {
            @Override
            public @Nullable T getValue(E entity, Vec3 pos) {
                return getter.apply(entity);
            }

            @Override
            public boolean setValue(T value, E entity, Vec3 pos) {
                var old = getter.apply(entity);
                setter.accept(entity, value);
                return !Objects.equals(old, getter.apply(entity));
            }
        };
    }

    @Nullable
    T getValue(E entity, Vec3 pos);
    boolean setValue(T value, E entity, Vec3 pos);
}
