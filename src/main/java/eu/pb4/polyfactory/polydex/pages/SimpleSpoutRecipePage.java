package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.fluid.SimpleDrainRecipe;
import eu.pb4.polyfactory.recipe.fluid.SimpleSpoutRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;

import java.util.List;

public class SimpleSpoutRecipePage extends SpoutRecipePage<SimpleSpoutRecipe> {
    public SimpleSpoutRecipePage(RecipeEntry<SimpleSpoutRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected ItemStack getResultStack() {
        return this.recipe.output();
    }

    @Override
    protected List<FluidStack<?>> getBaseFluids() {
        return this.recipe.fluidInput();
    }
    @Override
    protected CountedIngredient getBaseIngredient() {
        return new CountedIngredient(this.recipe.item(), 1, ItemStack.EMPTY);
    }
}
