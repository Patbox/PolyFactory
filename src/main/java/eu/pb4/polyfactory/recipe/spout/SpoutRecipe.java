package eu.pb4.polyfactory.recipe.spout;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;

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

    Holder<SoundEvent> soundEvent();
    double time(SingleItemWithFluid input);
    default double coolingTime(SingleItemWithFluid input) {
        return 0;
    }

    @Override
    default RecipeType<SpoutRecipe> getType() {
        return FactoryRecipeTypes.SPOUT;
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
