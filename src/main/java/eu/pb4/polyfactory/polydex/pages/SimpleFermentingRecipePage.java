package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.recipe.fermenting.SimpleFermentingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public class SimpleFermentingRecipePage extends FermenterRecipePage<SimpleFermentingRecipe> {

    private final List<PolydexStack<ItemStack>> outputStacks;

    public SimpleFermentingRecipePage(RecipeHolder<SimpleFermentingRecipe> recipe) {
        super(recipe);
        this.outputStacks = List.of(PolydexCompatImpl.createOutput(recipe.value().output()));
    }

    @Override
    protected Ingredient getItemInput() {
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
}
