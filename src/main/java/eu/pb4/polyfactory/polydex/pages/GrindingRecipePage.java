package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import eu.pb4.polyfactory.recipe.PressRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GrindingRecipePage extends AbstractRecipePolydexPage<GrindingRecipe> {
    private static final ItemStack ICON = FactoryItems.GRINDER_BLOCK.getDefaultStack();
    private final PolydexStack<?>[] output;

    public GrindingRecipePage(GrindingRecipe recipe) {
        super(recipe);
        this.output = PolydexCompatImpl.createOutput(this.recipe.output());
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
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return ICON;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return this.recipe.output().get(0).stack();
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        layer.setIngredient(2, 2, this.recipe.input());

        var i = 0;
        for (; i < this.output.length; i++) {
            layer.setOutput(4 + i, 2, this.output[i]);
        }
        for (; i < 3; i++) {
            layer.setEmpty(4 + i, 2);
        }
    }
}
