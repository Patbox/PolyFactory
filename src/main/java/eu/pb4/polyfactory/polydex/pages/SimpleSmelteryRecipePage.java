package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.smeltery.SimpleSmelteryRecipe;
import java.util.List;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

public class SimpleSmelteryRecipePage extends SmelteryRecipePage<SimpleSmelteryRecipe> {
    public SimpleSmelteryRecipePage(RecipeHolder<SimpleSmelteryRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected Ingredient getInput() {
        return this.recipe.ingredient();
    }

    @Override
    protected List<FluidStack<?>> getFluidOutput() {
        return this.recipe.output();
    }
}
