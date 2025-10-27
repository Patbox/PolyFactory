package eu.pb4.polyfactory.recipe;

import eu.pb4.polyfactory.recipe.input.GrindingInput;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface GrindingRecipe extends Recipe<GrindingInput> {
    default RecipeType<GrindingRecipe> getType() {
        return FactoryRecipeTypes.GRINDING;
    }

    default RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    default boolean isIgnoredInRecipeBook() {
        return true;
    }

    default IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.NONE;
    }

    @Override
    default ItemStack craft(GrindingInput input, RegistryWrapper.WrapperLookup registries) {
        var x = this.output(input, registries, null);
        return x != null ? x.getFirst() : ItemStack.EMPTY;
    }

    List<ItemStack> output(GrindingInput input, RegistryWrapper.WrapperLookup registryManager, @Nullable Random random);

    double grindTime(GrindingInput input);

    double minimumSpeed(GrindingInput input);

    double optimalSpeed(GrindingInput input);
}
