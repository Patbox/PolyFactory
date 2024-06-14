package eu.pb4.polyfactory.util.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

public interface MinimalSidedRecipeInventory extends MinimalSidedInventory, RecipeInput {
    @Override
    default ItemStack getStackInSlot(int slot) {
        return getStack(slot);
    }

    @Override
    default int getSize() {
        return this.size();
    }

    @Override
    default boolean isEmpty() {
        return MinimalSidedInventory.super.isEmpty();
    }
}
