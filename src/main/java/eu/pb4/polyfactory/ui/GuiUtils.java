package eu.pb4.polyfactory.ui;

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

    public static final void playClickSound(ServerPlayerEntity player) {
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
    }
}
