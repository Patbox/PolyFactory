package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidInstance;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.RecipeInput;

import java.util.List;

public record MixingInput(List<ItemStack> stacks, Object2LongMap<FluidInstance<?>> fluids) implements RecipeInput {
    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.stacks.get(slot);
    }

    @Override
    public int getSize() {
        return this.stacks.size();
    }

    public CraftingRecipeInput asCraftingRecipeInput() {
        return CraftingRecipeInput.create(2, 3, this.stacks);
    }

    public long getFluid(FluidInstance<?> type) {
        return this.fluids.getOrDefault(type, 0);
    }
}
