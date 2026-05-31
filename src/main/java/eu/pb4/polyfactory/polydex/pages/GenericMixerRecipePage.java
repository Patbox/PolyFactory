package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class GenericMixerRecipePage extends MixerRecipePage<GenericMixingRecipe> {

    private final List<PolydexStack<ItemStack>> outputStacks;

    public GenericMixerRecipePage(RecipeHolder<GenericMixingRecipe> recipe) {
        super(recipe);
        this.outputStacks = List.of(PolydexCompatImpl.createOutput(recipe.value().output()));
    }

    @Override
    protected List<FluidInputStack> getFluidInput() {
        return this.recipe.fluidInput();
    }

    @Override
    protected List<CountedIngredient> getItemInput() {
        return this.recipe.input();
    }

    @Override
    protected List<FluidStack<?>> getFluidOutput() {
        return this.recipe.fluidOutput();
    }

    @Override
    protected List<PolydexStack<ItemStack>> getItemOutput() {
        return this.outputStacks;
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
