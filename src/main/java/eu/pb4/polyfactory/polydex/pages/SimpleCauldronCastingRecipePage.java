package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.casting.SimpleCastingRecipe;
import eu.pb4.polyfactory.recipe.casting.SimpleCauldronCastingRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;

public class SimpleCauldronCastingRecipePage extends CauldronCastingRecipePage<SimpleCauldronCastingRecipe> {
    public SimpleCauldronCastingRecipePage(RecipeEntry<SimpleCauldronCastingRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected ItemStack getResultStack() {
        return this.recipe.output();
    }

    @Override
    protected FluidStack<?> getBaseFluid() {
        return this.recipe.fluidInput();
    }
}
