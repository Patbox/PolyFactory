package eu.pb4.polyfactory.recipe.press;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.recipe.input.PressInput;
import java.util.List;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public interface PressRecipe extends Recipe<PressInput> {
    double minimumSpeed();

    void applyRecipeUse(PressBlockEntity inventory, Level world);

    default List<FluidStack<?>> outputFluids(PressInput input) {
        return List.of();
    }

    @Override
    default RecipeType<PressRecipe> getType() {
        return FactoryRecipeTypes.PRESS;
    }

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
