package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.BrewingMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;

import java.util.List;

public class BrewingMixerRecipePage extends MixerRecipePage<BrewingMixingRecipe> {

    public BrewingMixerRecipePage(RecipeEntry<BrewingMixingRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected List<FluidInputStack> getFluidInput() {
        return List.of(new FluidInputStack(this.recipe.from(), this.recipe.minimumResult(), this.recipe.minimumResult()));
    }

    @Override
    protected List<CountedIngredient> getItemInput() {
        return List.of(new CountedIngredient(this.recipe.ingredient(), 1, ItemStack.EMPTY));
    }

    @Override
    protected List<FluidStack<?>> getFluidOutput() {
        return List.of(this.recipe.to().stackOf(this.recipe.minimumResult()));
    }

    @Override
    protected ItemStack getItemOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    protected float getMaxTemperature() {
        return this.recipe.maxTemperature();
    }

    @Override
    protected float getMinimumTemperature() {
        return this.recipe.minimumTemperature();
    }

    @Override
    public int priority() {
        return -10;
    }
}
