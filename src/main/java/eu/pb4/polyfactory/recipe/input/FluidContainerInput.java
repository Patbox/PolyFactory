package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidHolder;
import eu.pb4.polyfactory.fluid.FluidInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.ToLongFunction;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record FluidContainerInput(ToLongFunction<FluidInstance<?>> fluidsAmount, List<FluidInstance<?>> fluids, long stored, long capacity, float temperature) implements RecipeInput {
    public static final FluidContainerInput EMPTY = new FluidContainerInput(Object2LongMaps.emptyMap(), List.of(), 0, 0, 0);
    public static FluidContainerInput of(@Nullable FluidHolder fluidContainer) {
        return of(fluidContainer, 0);
    }
    public static FluidContainerInput of(@Nullable FluidHolder fluidContainer, float temperature) {
        return fluidContainer != null ? new FluidContainerInput(fluidContainer::get, fluidContainer.fluids(), fluidContainer.stored(), fluidContainer.capacity(), temperature) :
                EMPTY;
    }

    public long get(FluidInstance<?> type) {
        return this.fluidsAmount.applyAsLong(type);
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return stored == 0;
    }
}
