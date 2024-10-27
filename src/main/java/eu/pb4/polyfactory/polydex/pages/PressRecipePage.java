package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.press.GenericPressRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PressRecipePage extends PrioritizedRecipePage<GenericPressRecipe> {
    private static final ItemStack ICON = FactoryItems.PRESS.getDefaultStack();
    private final List<PolydexIngredient<?>> ingredients;
    private final PolydexStack<?>[] output;

    public PressRecipePage(RecipeEntry<GenericPressRecipe> recipe) {
        super(recipe);
        this.ingredients = PolydexCompatImpl.createIngredients(this.recipe.inputA(), this.recipe.inputB());
        this.output = PolydexCompatImpl.createOutput(this.recipe.output());
    }

    @Override
    public ItemStack getOutput(@Nullable PolydexEntry polydexEntry, MinecraftServer minecraftServer) {
        return this.recipe.output().getFirst().stack().copy();
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return PolydexTextures.PRESS;
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        for (var i : this.output) {
            if (entry.isPartOf(i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return this.recipe.output().get(0).stack();
    }

    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return ICON;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return ingredients;
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        layer.setIngredient(3, 1, this.ingredients.get(0));
        layer.setIngredient(5, 1, this.ingredients.get(1));
        layer.setOutput(4, 3, this.output);
    }
}
