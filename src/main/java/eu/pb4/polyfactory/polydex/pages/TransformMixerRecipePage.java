package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.util.ItemComponentPredicate;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.TransformMixingRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class TransformMixerRecipePage extends MixerRecipePage<TransformMixingRecipe> {

    private final PolydexStack<ItemStack> output;

    public TransformMixerRecipePage(RecipeHolder<TransformMixingRecipe> recipe) {
        super(recipe);
        this.output = PolydexStack.of(this.recipe.output().create());
    }

    @Override
    protected List<FluidInputStack> getFluidInput() {
        return this.recipe.fluidInput();
    }

    @Override
    protected List<CountedIngredient> getItemInput() {
        var list = new ArrayList<CountedIngredient>(this.recipe.input().size() + 1);
        list.add(new CountedIngredient(Optional.of(this.recipe.base()), ItemComponentPredicate.EMPTY, 1, Optional.empty()));
        list.addAll(this.recipe.input());
        return list;
    }

    @Override
    protected List<FluidStack<?>> getFluidOutput() {
        return this.recipe.fluidOutput();
    }

    @Override
    protected List<PolydexStack<ItemStack>> getItemOutput() {
        return List.of(this.output);
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
