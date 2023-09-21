package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MixerRecipePage extends AbstractRecipePolydexPage<GenericMixingRecipe> {
    private static final ItemStack ICON = FactoryItems.MIXER.getDefaultStack();
    private final List<PolydexIngredient<?>> ingredients;

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return PolydexTextures.MIXER;
    }

    public MixerRecipePage(RecipeEntry<GenericMixingRecipe> recipe) {
        super(recipe);
        this.ingredients = PolydexCompatImpl.createIngredients(this.recipe.input());
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
        for (int i = 0; i < 6; i++) {
            if (i < this.ingredients.size()) {
                layer.setIngredient(2 + i % 2, 1 + i / 2, this.ingredients.get(i));
            } else {
                layer.setEmpty(2 + i % 2, 1 + i / 2);
            }
        }
        layer.setOutput(6, 2, this.recipe.output());
        layer.set(4, 3, GuiTextures.FLAME_OFFSET_RIGHT.get(Math.max(this.recipe.minimumTemperature(), 0)).getItemStack(), GuiTextures.FLAME_OFFSET_RIGHT.get(Math.min(this.recipe.maxTemperature(), 1)).getItemStack());
    }
}
