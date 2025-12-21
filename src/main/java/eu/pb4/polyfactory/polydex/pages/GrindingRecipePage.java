package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

public abstract class GrindingRecipePage<T extends GrindingRecipe> extends PrioritizedRecipePage<T> {
    private static final ItemStack ICON = FactoryItems.GRINDER.getDefaultInstance();

    public GrindingRecipePage(RecipeHolder<T> recipe) {
        super(recipe);
    }

    @Override
    public @Nullable Component texture(ServerPlayer player) {
        return PolydexTextures.GRINDER;
    }

    @Override
    public ItemStack typeIcon(ServerPlayer player) {
        return ICON;
    }
}
