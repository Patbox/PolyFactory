package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SlotGuiInterface;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class GuiUtils {
    public static final GuiElement EMPTY = GuiElement.EMPTY;

    public static void playClickSound(ServerPlayer player) {
        FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
    }


    public static  <T extends Enum<T>> GuiElementInterface createIteratingButton(Supplier<T> supplier, Consumer<T> consumer, T[] values, Function<T, GuiElementBuilder> builderFunction) {
        return new GuiElementInterface() {
            @Override
            public ItemStack getItemStack() {
                return builderFunction.apply(supplier.get()).asStack();
            }

            @Override
            public ClickCallback getGuiCallback() {
                return (i, clickType, slotActionType, slotGuiInterface) -> {
                    if (clickType.isLeft) {
                        GuiUtils.playClickSound(slotGuiInterface.getPlayer());
                        consumer.accept(values[(values.length + supplier.get().ordinal() + (clickType.shift ? -1 : 1)) % values.length]);
                    }
                };
            }
        };
    }

    public static void drawFlatNumbers(SlotGuiInterface gui, int position, int value, int size, int color, boolean leadingZero) {
        drawNumberInternal(gui, position, value, size, color, leadingZero, GuiTextures.NUMBERS_FLAT_24);
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

    public static GuiElementInterface createDynamicButton(Supplier<ItemStack> itemSupplier, Consumer<ClickType> clickTypeConsumer) {
        return createDynamicButton(itemSupplier, (index, type, action, gui) -> clickTypeConsumer.accept(type));
    }

    public static GuiElementInterface createDynamicButton(Supplier<ItemStack> itemSupplier, GuiElementInterface.ClickCallback callback) {
        return new GuiElementInterface() {
            @Override
            public ItemStack getItemStack() {
                return itemSupplier.get();
            }

            @Override
            public ClickCallback getGuiCallback() {
                return callback;
            }
        };
    }
}

