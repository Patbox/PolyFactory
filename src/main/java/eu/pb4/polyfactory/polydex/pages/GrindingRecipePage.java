package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class GrindingRecipePage<T extends GrindingRecipe> extends PrioritizedRecipePage<T> {
    private static final ItemStack ICON = FactoryItems.GRINDER.getDefaultStack();

    public GrindingRecipePage(RecipeEntry<T> recipe) {
        super(recipe);
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return PolydexTextures.GRINDER;
    }

    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return ICON;
    }
}
