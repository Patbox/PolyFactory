package eu.pb4.polyfactory.recipe.mixing;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.world.World;

import java.util.List;

public interface MixingRecipe extends Recipe<MixingInput>, PolymerRecipe {

     Iterable<ItemStack> remainders();
     void applyRecipeUse(MixerBlockEntity inventory, World world);

    @Override
    default RecipeType<?> getType() {
        return FactoryRecipeTypes.MIXER;
    }

    double optimalSpeed();
    double minimumSpeed();
    float minimumTemperature();
    float maxTemperature();

    double time();
    default List<FluidInputStack> fluids() {
        return List.of();
    }

    @Override
    default ItemStack createIcon() {
        return FactoryItems.MIXER.getDefaultStack();
    }
}
