package eu.pb4.polyfactory.recipe.input;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SingleItemWithTemperature(ItemStack stack, float temperature, ServerLevel world) implements RecipeInput {
    @Override
    public ItemStack getItem(int slot) {
        return stack;
    }

    @Override
    public int size() {
        return 1;
    }
}
