package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.util.ItemComponentPredicate;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.BrewingMixingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public class BrewingMixerRecipePage extends MixerRecipePage<BrewingMixingRecipe> {

    public BrewingMixerRecipePage(RecipeHolder<BrewingMixingRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected List<FluidInputStack> getFluidInput() {
        return List.of(new FluidInputStack(this.recipe.from(), this.recipe.minimumResult(), this.recipe.minimumResult()));
    }

    @Override
    protected List<CountedIngredient> getItemInput() {
        return List.of(new CountedIngredient(Optional.of(this.recipe.ingredient()), ItemComponentPredicate.EMPTY, 1, Optional.empty()));
    }

    @Override
    protected List<FluidStack<?>> getFluidOutput() {
        return List.of(this.recipe.to().stackOf(this.recipe.minimumResult()));
    }

    @Override
    protected List<PolydexStack<ItemStack>> getItemOutput() {
        return List.of();
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
    protected double getOptimalSpeed() {
        return this.recipe.optimalSpeed();
    }

    @Override
    protected double getMinimumSpeed() {
        return this.recipe.minimumSpeed();
    }

    @Override
    public int priority() {
        return -10;
    }
}
