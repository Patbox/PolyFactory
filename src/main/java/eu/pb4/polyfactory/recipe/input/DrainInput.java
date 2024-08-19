package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

import java.util.List;

public record DrainInput(ItemStack stack, ItemStack catalyst, Object2LongMap<FluidInstance<?>> fluidsAmount, List<FluidInstance<?>> fluids, long stored, long capacity,
                         boolean isPlayer) implements RecipeInput {
    public static DrainInput of(ItemStack stack, ItemStack catalyst, FluidContainer fluidContainer, boolean isPlayer) {
        return new DrainInput(stack, catalyst, fluidContainer.asMap(), fluidContainer.fluids(), fluidContainer.stored(), fluidContainer.capacity(), isPlayer);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? stack : catalyst;
    }
    @Override
    public int getSize() {
        return 2;
    }
    public long getFluid(FluidInstance<?> type) {
        return this.fluidsAmount.getOrDefault(type, 0);
    }
}
