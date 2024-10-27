package eu.pb4.polyfactory.recipe.mixing;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.world.World;

import java.util.List;

public interface MixingRecipe extends Recipe<MixingInput> {
     Iterable<ItemStack> remainders(MixingInput input);
     void applyRecipeUse(MixerBlockEntity inventory, World world);

    @Override
    default RecipeType<MixingRecipe> getType() {
        return FactoryRecipeTypes.MIXER;
    }

    double optimalSpeed(MixingInput input);
    double minimumSpeed(MixingInput input);
    float minimumTemperature(MixingInput input);
    float maxTemperature(MixingInput input);

    double time(MixingInput input);
    default List<FluidStack<?>> fluidOutput(MixingInput input) {
        return List.of();
    }
    default List<FluidInputStack> fluidInput(MixingInput input) {
        return List.of();
    }

    @Override
    default RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    @Override
    default boolean isIgnoredInRecipeBook() {
        return true;
    }

    @Override
    default IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.NONE;
    }
}
