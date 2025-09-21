package eu.pb4.polyfactory.recipe.press;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.recipe.input.PressInput;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.world.World;

import java.util.List;

public interface PressRecipe extends Recipe<PressInput> {
    double minimumSpeed();

    void applyRecipeUse(PressBlockEntity inventory, World world);

    default List<FluidStack<?>> outputFluids(PressInput input) {
        return List.of();
    }

    @Override
    default RecipeType<PressRecipe> getType() {
        return FactoryRecipeTypes.PRESS;
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
