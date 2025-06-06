package eu.pb4.polyfactory.recipe.spout;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

import java.util.List;

public interface SpoutRecipe extends Recipe<SingleItemWithFluid> {
    static double getTime(FluidInstance<?> fluidInstance, long amount) {
        return Math.max(amount / fluidInstance.getFlowSpeedMultiplier(null) / fluidInstance.getMaxFlow(null) * 1.5d, 1);
    }

    static double getTime(List<FluidStack<?>> stacks) {
        var out = 0d;
        for (var stack : stacks) {
            out += getTime(stack.instance(), stack.amount());
        }
        return out;
    }

    default int decreasedInputItemAmount(SingleItemWithFluid input) {
        return 1;
    }

    default int damageInputItemAmount(SingleItemWithFluid input) {
        return 0;
    }

    List<FluidStack<?>> fluidInput(SingleItemWithFluid input);

    RegistryEntry<SoundEvent> soundEvent();
    double time(SingleItemWithFluid input);
    default double coolingTime(SingleItemWithFluid input) {
        return 0;
    }

    @Override
    default RecipeType<SpoutRecipe> getType() {
        return FactoryRecipeTypes.SPOUT;
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
