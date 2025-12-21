package eu.pb4.polyfactory.recipe.input;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record PressInput(ItemStack input, ItemStack pattern) implements RecipeInput {
    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? input : pattern;
    }

    @Override
    public int size() {
        return 2;
    }
}
