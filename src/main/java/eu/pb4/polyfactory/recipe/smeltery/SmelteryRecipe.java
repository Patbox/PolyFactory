package eu.pb4.polyfactory.recipe.smeltery;

import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.List;

public interface SmelteryRecipe extends Recipe<SingleStackRecipeInput> {
    @Override
    default ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return ItemStack.EMPTY;
    }

    List<FluidStack<?>> output(SingleStackRecipeInput input, World world);

    int time(SingleStackRecipeInput input, World world);

    @Override
    default RecipeType<? extends Recipe<SingleStackRecipeInput>> getType() {
        return FactoryRecipeTypes.SMELTERY;
    }

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
