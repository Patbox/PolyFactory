package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.RatedFluidMixingMixingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class RatedFluidMixerRecipePage extends MixerRecipePage<RatedFluidMixingMixingRecipe> {
    public RatedFluidMixerRecipePage(RecipeHolder<RatedFluidMixingMixingRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected List<FluidInputStack> getFluidInput() {
        return this.recipe.fluidInputs();
    }

    @Override
    protected List<CountedIngredient> getItemInput() {
        return List.of();
    }

    @Override
    protected List<FluidStack<?>> getFluidOutput() {
        return this.recipe.fluidOutput();
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
}
