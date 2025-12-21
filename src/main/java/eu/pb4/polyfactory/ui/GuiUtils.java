package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class GuiUtils {
    public static final GuiElement EMPTY = GuiElement.EMPTY;

    public static void playClickSound(ServerPlayer player) {
        FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
    }


    public static void drawFlatNumbers(SlotGuiInterface gui, int position, int value, int size, int color, boolean leadingZero) {
        drawNumberInternal(gui, position, value, size, color, leadingZero, GuiTextures.NUMBERS_FLAT);
    }

    //public static void drawShadowNumbers(SlotGuiInterface gui, int position, int value, int size, int color, boolean leadingZero) {
    //    drawNumberInternal(gui, position, value, size, color, leadingZero, GuiTextures.NUMBERS_SHADOW);
    //}

    private static void drawNumberInternal(SlotGuiInterface gui, int position, int value, int size, int color, boolean leadingZero, IntFunction<GuiElementBuilder>[] numbers) {
        var negative = value < 0;
        for (int i = size - 1; i >= 0; i--)  {
            if (leadingZero && negative) {
                gui.setSlot(position, numbers[10].apply(color).hideTooltip());
                negative = false;
                continue;
            }
            if (!leadingZero && value == 0 && i != size -1) {
                if (negative) {
                    gui.setSlot(position + i, numbers[10].apply(color).hideTooltip());
                    negative = false;
                    continue;
                }
                gui.setSlot(position + i, EMPTY);
                continue;
            }

            var number = value % 10;
            value /= 10;
            gui.setSlot(position + i,  numbers[Math.abs(number)].apply(color).hideTooltip());
        }
    }
}

