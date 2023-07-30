package eu.pb4.polyfactory.ui;

import eu.pb4.polydex.impl.book.ui.GuiTextures;
import eu.pb4.polydex.impl.book.ui.PageAware;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public class GuiUtils {
    public static final GuiElement EMPTY = GuiElement.EMPTY;
    public static final GuiElement FILLER = Util.make(() -> {
        var b = new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                .setName(Text.empty())
                .hideFlags();
        b.getOrCreateNbt().putBoolean("polydex:filler", true);
        return b.build();
    });

    public static GuiElement backButton(ServerPlayerEntity player, Runnable callback, boolean back) {
        return new GuiElementBuilder(Items.STRUCTURE_VOID)
                .setName(Text.translatable(back ? "gui.back" : "test.polydex.close").formatted(Formatting.RED))
                .hideFlags()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    callback.run();
                }).build();
    }

    public static final void playClickSound(ServerPlayerEntity player) {
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
    }

    public static GuiElement nextPage(ServerPlayerEntity player, PageAware gui) {
        return new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Text.translatable("spectatorMenu.next_page").formatted(Formatting.WHITE))
                .hideFlags()
                .setSkullOwner(GuiTextures.GUI_NEXT_PAGE)
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.nextPage();
                }).build();
    }

    public static GuiElement previousPage(ServerPlayerEntity player, PageAware gui) {
        return new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Text.translatable("spectatorMenu.previous_page").formatted(Formatting.WHITE))
                .hideFlags()
                .setSkullOwner(GuiTextures.GUI_PREVIOUS_PAGE)
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.previousPage();
                }).build();
    }
}
