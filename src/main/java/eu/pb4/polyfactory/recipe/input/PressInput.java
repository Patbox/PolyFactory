package eu.pb4.polyfactory.recipe.input;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

public record PressInput(ItemStack input, ItemStack pattern) implements RecipeInput {
    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? input : pattern;
    }

    @Override
    public int size() {
        return 2;
    }
}
