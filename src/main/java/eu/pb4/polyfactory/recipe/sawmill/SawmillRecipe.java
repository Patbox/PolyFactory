package eu.pb4.polyfactory.recipe.sawmill;

import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.SawmillInput;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SawmillRecipe extends Recipe<SawmillInput> {
    default RecipeType<SawmillRecipe> getType() {
        return FactoryRecipeTypes.SAWMILL;
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
    default ItemStack assemble(SawmillInput input) {
        var x = this.output(input,  null);
        return x != null ? x.getFirst() : ItemStack.EMPTY;
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    List<ItemStack> output(SawmillInput input, @Nullable RandomSource random);

    double time(SawmillInput input);

    double minimumSpeed(SawmillInput input);

    double optimalSpeed(SawmillInput input);
}
