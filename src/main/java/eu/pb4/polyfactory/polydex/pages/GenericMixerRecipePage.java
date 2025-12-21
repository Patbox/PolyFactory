package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GenericMixerRecipePage extends MixerRecipePage<GenericMixingRecipe> {

    public GenericMixerRecipePage(RecipeHolder<GenericMixingRecipe> recipe) {
        super(recipe);
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
