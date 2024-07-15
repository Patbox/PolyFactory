package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record FluidsPage(Identifier identifier, PolydexIngredient<ItemStack> item, PolydexStack<ItemStack> itemResult, PolydexStack<FluidInstance<?>> fluidResult) implements PolydexPage {
    public static final ItemStack ICON = new GuiElementBuilder(FactoryItems.DRAIN)
            .setName(Text.translatable("polydex_category.polyfactory.fluids"))
            .asStack();
    @Override
    public ItemStack typeIcon(ServerPlayerEntity player) {
        return ICON;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry entry, ServerPlayerEntity player) {
        return GuiElementBuilder.from(FactoryModels.FLAT_FULL.get(fluidResult.getBacking())).hideDefaultTooltip().setName(fluidResult.getName()).asStack();
    }

    @Override
    public void createPage(@Nullable PolydexEntry entry, ServerPlayerEntity player, PageBuilder layer) {
        layer.setIngredient(2, 2, item);
        if (!layer.hasTextures()) {
            layer.set(4, 2, new GuiElementBuilder(Items.ARROW).hideTooltip());
        }
        layer.setIngredient(6, 3, itemResult);

        layer.setOutput(6, 2, fluidResult);
    }

    @Override
    public @Nullable Text texture(ServerPlayerEntity player) {
        return InternalPageTextures.STONECUTTING;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return List.of(item);
    }

    @Override
    public List<PolydexCategory> categories() {
        return List.of(PolydexCompatImpl.FLUIDS_CATEGORY);
    }

    @Override
    public boolean isOwner(MinecraftServer server, PolydexEntry entry) {
        return entry.isPartOf(itemResult) || entry.isPartOf(fluidResult);
    }
}