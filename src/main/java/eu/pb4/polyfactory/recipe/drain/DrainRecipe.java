package eu.pb4.polyfactory.recipe.drain;

import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

import java.util.List;

public interface DrainRecipe extends Recipe<DrainInput> {
    List<FluidStack<?>> fluidOutput(DrainInput input);
    List<FluidStack<?>> fluidInput(DrainInput input);

    default int decreasedInputItemAmount(DrainInput input) {
        return 1;
    }

    RegistryEntry<SoundEvent> soundEvent();
    double time(DrainInput input);

    default boolean requirePlayer() {
        return false;
    }

    @Override
    default RecipeType<DrainRecipe> getType() {
        return FactoryRecipeTypes.DRAIN;
    };

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
