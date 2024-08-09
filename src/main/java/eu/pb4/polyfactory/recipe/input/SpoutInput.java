package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

import java.util.List;

public record SpoutInput(ItemStack stack, Object2LongMap<FluidInstance<?>> fluidsAmount, List<FluidInstance<?>> fluids) implements RecipeInput {
    public static SpoutInput of(ItemStack stack, FluidContainer fluidContainer) {
        return new SpoutInput(stack, fluidContainer.asMap(), fluidContainer.orderList());
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stack;
    }
    @Override
    public int getSize() {
        return 1;
    }
    public long getFluid(FluidInstance<?> type) {
        return this.fluidsAmount.getOrDefault(type, 0);
    }
}
