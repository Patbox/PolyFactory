package eu.pb4.polyfactory.recipe.casting;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;

public interface CauldronCastingRecipe extends Recipe<FluidContainerInput> {
    static double getTime(FluidInstance<?> fluidInstance, long amount) {
        return Math.max(30 * amount / fluidInstance.getFlowSpeedMultiplier(null) / fluidInstance.getMaxFlow(null), 1);
    }
    FluidStack<?> fluidInput(FluidContainerInput input);
    Holder<SoundEvent> soundEvent();
    double time(FluidContainerInput input);
    double coolingTime(FluidContainerInput input);

    @Override
    default RecipeType<CauldronCastingRecipe> getType() {
        return FactoryRecipeTypes.CASTING_CAULDRON;
    };

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }
}
