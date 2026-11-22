package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.SawmillBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record SawmillInput(ItemStack stack, SawmillBlock.BladeAngle angle) implements RecipeInput {
    @Override
    public ItemStack getItem(int slot) {
        return stack;
    }

    @Override
    public int size() {
        return 1;
    }
}
