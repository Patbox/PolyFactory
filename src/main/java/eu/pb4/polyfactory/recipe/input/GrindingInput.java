package eu.pb4.polyfactory.recipe.input;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;

import java.util.List;

public record GrindingInput(ItemStack stack) implements RecipeInput {
    @Override
    public ItemStack getStackInSlot(int slot) {
        return stack;
    }

    @Override
    public int size() {
        return 1;
    }
}
