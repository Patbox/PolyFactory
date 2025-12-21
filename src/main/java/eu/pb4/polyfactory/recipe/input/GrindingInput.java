package eu.pb4.polyfactory.recipe.input;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record GrindingInput(ItemStack stack) implements RecipeInput {
    @Override
    public ItemStack getItem(int slot) {
        return stack;
    }

    @Override
    public int size() {
        return 1;
    }
}
