package eu.pb4.polyfactory.recipe.fermenting;

import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.SingleItemWithTemperature;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.List;

public interface FermentingRecipe extends Recipe<SingleItemWithTemperature> {
    @Override
    default RecipeType<FermentingRecipe> getType() {
        return FactoryRecipeTypes.FERMENTER;
    }

    double time(SingleItemWithTemperature input);
    List<FluidStack<?>> fluidOutput(SingleItemWithTemperature input);

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default String group() {
        return "";
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Deprecated
    @Override
    default ItemStack assemble(SingleItemWithTemperature input) {
        return assembleStacks(input, RandomSource.createThreadLocalInstance(), false).getFirst();
    }

    List<ItemStack> assembleStacks(SingleItemWithTemperature input, RandomSource randomSource, boolean applyChance);

    default ItemStack getRemainingItem(SingleItemWithTemperature input, RandomSource random) {
        var remainer = input.stack().getCraftingRemainder();

        return remainer != null ? remainer.create() : ItemStack.EMPTY;
    }
}
