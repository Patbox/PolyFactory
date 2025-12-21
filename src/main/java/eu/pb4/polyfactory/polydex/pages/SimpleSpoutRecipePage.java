package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.spout.SimpleSpoutRecipe;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class SimpleSpoutRecipePage extends SpoutRecipePage<SimpleSpoutRecipe> {
    public SimpleSpoutRecipePage(RecipeHolder<SimpleSpoutRecipe> recipe) {
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
        return this.recipe.item();
    }
}
