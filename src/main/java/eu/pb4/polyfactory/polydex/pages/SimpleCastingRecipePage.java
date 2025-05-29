package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.casting.SimpleCastingRecipe;
import eu.pb4.polyfactory.recipe.spout.SimpleSpoutRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;

import java.util.List;

public class SimpleCastingRecipePage extends CastingRecipePage<SimpleCastingRecipe> {
    public SimpleCastingRecipePage(RecipeEntry<SimpleCastingRecipe> recipe) {
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
    @Override
    protected CountedIngredient getBaseIngredient() {
        return this.recipe.item();
    }
}
