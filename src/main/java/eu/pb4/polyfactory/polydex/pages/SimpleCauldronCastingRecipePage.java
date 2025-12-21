package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.casting.SimpleCastingRecipe;
import eu.pb4.polyfactory.recipe.casting.SimpleCauldronCastingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class SimpleCauldronCastingRecipePage extends CauldronCastingRecipePage<SimpleCauldronCastingRecipe> {
    public SimpleCauldronCastingRecipePage(RecipeHolder<SimpleCauldronCastingRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected ItemStack getResultStack() {
        return this.recipe.output();
    }

    @Override
    protected FluidStack<?> getBaseFluid() {
        return this.recipe.fluidInput();
    }
}
