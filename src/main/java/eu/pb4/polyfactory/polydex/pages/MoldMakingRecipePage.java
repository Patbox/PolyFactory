package eu.pb4.polyfactory.polydex.pages;

import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompatImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record MoldMakingRecipePage(Identifier identifier, PolydexStack<?> input, PolydexStack<?> output) implements PolydexPage {
    private static final ItemStack TYPE_ICON = FactoryItems.MOLDMAKING_TABLE.getDefaultInstance();


    public @Nullable Component texture(ServerPlayer player) {
        return InternalPageTextures.STONECUTTING;
    }

    @Override
    public List<PolydexIngredient<?>> ingredients() {
        return List.of(input);
    }

    @Override
    public List<PolydexCategory> categories() {
        return List.of(PolydexCompatImpl.MOLDMAKING_CATEGORY);
    }

    @Override
    public boolean isOwner(MinecraftServer minecraftServer, PolydexEntry polydexEntry) {
        return polydexEntry.isPartOf(this.output);
    }

    @Override
    public Identifier identifier() {
        return this.identifier;
    }

    public ItemStack typeIcon(ServerPlayer player) {
        return TYPE_ICON;
    }

    @Override
    public ItemStack entryIcon(@Nullable PolydexEntry polydexEntry, ServerPlayer serverPlayer) {
        return this.output.toItemStack(serverPlayer);
    }

    public void createPage(@Nullable PolydexEntry entry, ServerPlayer player, PageBuilder builder) {
        builder.setIngredient(2, 2, this.input);
        builder.setOutput(6, 2, this.output);
    }
}