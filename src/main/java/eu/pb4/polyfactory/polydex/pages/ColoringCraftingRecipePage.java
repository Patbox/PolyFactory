package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.impl.book.view.crafting.AbstractCraftingRecipePage;
import eu.pb4.polyfactory.recipe.ColoringCraftingRecipe;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jetbrains.annotations.Nullable;

public class ColoringCraftingRecipePage extends AbstractCraftingRecipePage<ColoringCraftingRecipe> {

    public ColoringCraftingRecipePage(RecipeHolder<ColoringCraftingRecipe> recipe) {
        super(recipe);
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        return recipe.input().getDefaultInstance();
    }

    @Override
    protected SlotDisplay getStacksAt(ColoringCraftingRecipe coloringCraftingRecipe, int x, int y, PolydexEntry entry) {
        if (x == 0 && y == 0) {
            return new SlotDisplay.TagSlotDisplay(ConventionalItemTags.DYES);
        }
        if (x == 1 && y == 0) {
            return new SlotDisplay.ItemSlotDisplay(coloringCraftingRecipe.input());
        }

        return SlotDisplay.Empty.INSTANCE;
    }
}
