package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.smeltery.SimpleSmelteryRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;

import java.util.List;

public class SimpleSmelteryRecipePage extends SmelteryRecipePage<SimpleSmelteryRecipe> {
    public SimpleSmelteryRecipePage(RecipeEntry<SimpleSmelteryRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected Ingredient getInput() {
        return this.recipe.ingredient();
    }

    @Override
    protected List<FluidStack<?>> getFluidOutput() {
        return this.recipe.output();
    }
}
