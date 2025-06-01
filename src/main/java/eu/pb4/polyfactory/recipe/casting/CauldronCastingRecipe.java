package eu.pb4.polyfactory.recipe.casting;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

public interface CauldronCastingRecipe extends Recipe<FluidContainerInput> {
    static double getTime(FluidInstance<?> fluidInstance, long amount) {
        return Math.max(30 * amount / fluidInstance.getFlowSpeedMultiplier(null) / fluidInstance.getMaxFlow(null), 1);
    }
    FluidStack<?> fluidInput(FluidContainerInput input);
    RegistryEntry<SoundEvent> soundEvent();
    double time(FluidContainerInput input);
    double coolingTime(FluidContainerInput input);

    @Override
    default RecipeType<CauldronCastingRecipe> getType() {
        return FactoryRecipeTypes.CASTING_CAULDRON;
    };

    @Override
    default IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.NONE;
    }

    @Override
    default RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    @Override
    default boolean isIgnoredInRecipeBook() {
        return true;
    }
}
