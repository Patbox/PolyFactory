package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.util.ItemComponentPredicate;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.TransformMixingRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class TransformMixerRecipePage extends MixerRecipePage<TransformMixingRecipe> {

    public TransformMixerRecipePage(RecipeHolder<TransformMixingRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected List<FluidInputStack> getFluidInput() {
        return this.recipe.fluidInput();
    }

    @Override
    protected List<CountedIngredient> getItemInput() {
        var list = new ArrayList<CountedIngredient>(this.recipe.input().size() + 1);
        list.add(new CountedIngredient(Optional.of(this.recipe.base()), ItemComponentPredicate.EMPTY, 1, ItemStack.EMPTY));
        list.addAll(this.recipe.input());
        return list;
    }

    @Override
    protected List<FluidStack<?>> getFluidOutput() {
        return this.recipe.fluidOutput();
    }

    @Override
    protected ItemStack getItemOutput() {
        return this.recipe.output();
    }

    @Override
    protected float getMaxTemperature() {
        return this.recipe.maxTemperature();
    }

    @Override
    protected float getMinimumTemperature() {
        return this.recipe.minimumTemperature();
    }
}
