package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.tool.UniversalFluidContainerItem;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.fabricmc.fabric.api.entity.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface FluidContainer {
    long get(FluidInstance<?> type);

    long set(FluidInstance<?> type, long amount);

    default boolean doesNotContain(FluidInstance<?> type) {
        return !contains(type);
    }

    default boolean contains(FluidInstance<?> type) {
        return this.get(type) > 0;
    }

    boolean canInsert(FluidInstance<?> type, long amount, boolean exact);

    long insert(FluidInstance<?> type, long amount, boolean exact);

    boolean canExtract(FluidInstance<?> type, long amount, boolean exact);

    long extract(FluidInstance<?> type, long amount, boolean exact);

    long capacity();

    long stored();

    Object2LongMap<FluidInstance<?>> asMap();

    List<FluidInstance<?>> fluids();

    void clear();

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

    default boolean isEmpty() {
        return this.stored() == 0;
    }

    default boolean canInsert(FluidStack<?> stack, boolean strict) {
        return canInsert(stack.instance(), stack.amount(), strict);
    }

    default boolean canExtract(FluidStack<?> stack, boolean strict) {
        return canExtract(stack.instance(), stack.amount(), strict);
    }

    default void insertExact(FluidInstance<?> instance, long amount) {
        insert(instance, amount, true);
    }

    default long insert(FluidStack<?> stack, boolean strict) {
        return insert(stack.instance(), stack.amount(), strict);
    }

    default long extract(FluidStack<?> stack, boolean strict) {
        return extract(stack.instance(), stack.amount(), strict);
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

    default int updateId() {
        return System.identityHashCode(this);
    }

}
