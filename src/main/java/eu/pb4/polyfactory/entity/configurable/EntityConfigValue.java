package eu.pb4.polyfactory.entity.configurable;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface EntityConfigValue<T, E extends Entity> {

    static <T, E extends Entity> EntityConfigValue<T, E> of(Function<E, T> getter, BiConsumer<E, T> setter) {
        return new EntityConfigValue<T, E>() {
            @Override
            public @Nullable T getValue(E entity, Vec3d pos) {
                return getter.apply(entity);
            }

            @Override
            public boolean setValue(T value, E entity, Vec3d pos) {
                var old = getter.apply(entity);
                setter.accept(entity, value);
                return !Objects.equals(old, getter.apply(entity));
            }
        };
    }

    @Nullable
    T getValue(E entity, Vec3d pos);
    boolean setValue(T value, E entity, Vec3d pos);
}
