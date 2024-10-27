package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.impl.book.view.crafting.AbstractCraftingRecipePage;
import eu.pb4.polyfactory.recipe.ColoringCraftingRecipe;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.display.SlotDisplay;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public class ColoringCraftingRecipePage extends AbstractCraftingRecipePage<ColoringCraftingRecipe> {

    public ColoringCraftingRecipePage(RecipeEntry<ColoringCraftingRecipe> recipe) {
        super(recipe);
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        return recipe.input().getDefaultStack();
    }

    @Override
    protected SlotDisplay getStacksAt(ColoringCraftingRecipe coloringCraftingRecipe, int x, int y, PolydexEntry entry) {
        if (x == 0 && y == 0) {
            return new SlotDisplay.TagSlotDisplay(ConventionalItemTags.DYES);
        }
        if (x == 1 && y == 0) {
            return new SlotDisplay.ItemSlotDisplay(coloringCraftingRecipe.input());
        }

        return SlotDisplay.EmptySlotDisplay.INSTANCE;
    }
}
