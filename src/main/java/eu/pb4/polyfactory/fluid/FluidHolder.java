package eu.pb4.polyfactory.fluid;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public interface FluidHolder {
    long get(FluidInstance<?> type);

    default boolean doesNotContain(FluidInstance<?> type) {
        return !contains(type);
    }

    default boolean contains(FluidInstance<?> type) {
        return this.get(type) > 0;
    }

    List<FluidInstance<?>> fluids();

    long stored();

    long capacity();

    @Nullable
    default FluidInstance<?> topFluid() {
        var fluids = fluids();
        return fluids.isEmpty() ? null : fluids.getLast();
    }

    @Nullable
    default FluidInstance<?> bottomFluid() {
        var fluids = fluids();
        return fluids.isEmpty() ? null : fluids.getFirst();
    }

    default boolean isEmpty() {
        return this.stored() == 0;
    }

    default float getFilledPercentage() {
        return (float) (((double) this.stored()) / this.capacity());
    }

    default boolean isNotEmpty() {
        return !this.isEmpty();
    }

    default boolean isFull() {
        return this.stored() >= this.capacity();
    }

    default boolean isNotFull() {
        return !isFull();
    }

    default long empty() {
        return Math.max(this.capacity() - this.stored(), 0);
    }

    default void provideRender(BiConsumer<FluidInstance<?>, Float> consumer) {
        forEach((a, b) -> consumer.accept(a, (float) (((double) b) / this.capacity())));
    }

    default void forEach(BiConsumer<FluidInstance<?>, Long> consumer) {
        for (var f : this.fluids()) {
            consumer.accept(f, this.get(f));
        }
    }

    default void forEachReversed(BiConsumer<FluidInstance<?>, Long> consumer) {
        for (var f : this.fluids().reversed()) {
            consumer.accept(f, this.get(f));
        }
    }
}
