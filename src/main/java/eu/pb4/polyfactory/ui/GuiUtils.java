package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.function.IntFunction;
import java.util.function.Supplier;

public class GuiUtils {
    public static final GuiElement EMPTY = GuiElement.EMPTY;

    public static void playClickSound(ServerPlayerEntity player) {
        FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.UI, 0.5f, 1);
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

