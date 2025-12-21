package eu.pb4.polyfactory.recipe;

import eu.pb4.polyfactory.recipe.input.GrindingInput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;

public interface GrindingRecipe extends Recipe<GrindingInput> {
    default RecipeType<GrindingRecipe> getType() {
        return FactoryRecipeTypes.GRINDING;
    }

    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    default boolean isSpecial() {
        return true;
    }

    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default ItemStack assemble(GrindingInput input, HolderLookup.Provider registries) {
        var x = this.output(input, registries, null);
        return x != null ? x.getFirst() : ItemStack.EMPTY;
    }

    List<ItemStack> output(GrindingInput input, HolderLookup.Provider registryManager, @Nullable RandomSource random);

    double grindTime(GrindingInput input);

    double minimumSpeed(GrindingInput input);

    double optimalSpeed(GrindingInput input);
}
