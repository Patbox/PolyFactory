package eu.pb4.polyfactory.recipe.drain;

import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;

public interface DrainRecipe extends Recipe<DrainInput> {
    List<FluidStack<?>> fluidOutput(DrainInput input);
    List<FluidStack<?>> fluidInput(DrainInput input);

    default int decreasedInputItemAmount(DrainInput input) {
        return 1;
    }

    Holder<SoundEvent> soundEvent();
    double time(DrainInput input);

    default boolean requirePlayer() {
        return false;
    }

    @Override
    default RecipeType<DrainRecipe> getType() {
        return FactoryRecipeTypes.DRAIN;
    };

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
}
