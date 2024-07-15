package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.impl.book.view.crafting.AbstractCraftingRecipePage;
import eu.pb4.polyfactory.recipe.ColoringCraftingRecipe;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;

public class ColoringCraftingRecipePage extends AbstractCraftingRecipePage<ColoringCraftingRecipe> {

    public ColoringCraftingRecipePage(RecipeEntry<ColoringCraftingRecipe> recipe) {
        super(recipe);
    }

    @Override
    protected Ingredient getStacksAt(ColoringCraftingRecipe coloringCraftingRecipe, int x, int y, PolydexEntry entry) {
        if (x == 0 && y == 0) {
            return Ingredient.fromTag(ConventionalItemTags.DYES);
        }
        if (x == 1 && y == 0) {
            return Ingredient.ofItems(coloringCraftingRecipe.input());
        }

        return Ingredient.EMPTY;
    }
}
