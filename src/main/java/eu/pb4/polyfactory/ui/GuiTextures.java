package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static eu.pb4.polyfactory.ui.UiResourceCreator.*;

public class GuiTextures {
    public static final Function<Text, Text> CRAFTER = background("crafter");
    public static final Function<Text, Text> GRINDER = background("grinder");
    public static final Function<Text, Text> PRESS = background("press");
    public static final Function<Text, Text> MIXER = background("mixer");
    public static final Function<Text, Text> MECHANICAL_DRAIN = background("mechanical_drain");
    public static final Function<Text, Text> MECHANICAL_SPOUT = background("mechanical_spout");
    public static final Function<Text, Text> MECHANICAL_SPOUT_NO_CONN = background("mechanical_spout_noconn");
    public static final Function<Text, Text> STEAM_ENGINE = background("steam_engine");
    public static final Function<Text, Text> CENTER_SLOT_GENERIC = background("center_slot");
    public static final Function<Text, Text> FILL3 = background("fill3");
    public static final Function<Text, Text> TEXT_INPUT = backgroundAnvil("text_input");
    public static final Supplier<GuiElementBuilder> EMPTY_BUILDER = icon16("empty");
    public static final GuiElement EMPTY = EMPTY_BUILDER.get().hideTooltip().build();
    public static final Supplier<GuiElementBuilder> POLYDEX_BUTTON = icon32("polydex");
    public static final Supplier<GuiElementBuilder> PLUS_BUTTON = icon32("button/plus");
    public static final Supplier<GuiElementBuilder> MINUS_BUTTON = icon32("button/minus");
    public static final Supplier<GuiElementBuilder> ACTIVATE_BUTTON = icon32("button/activate");
    public static final Supplier<GuiElementBuilder> ACTIVATE_BLOCKED_BUTTON = icon32("button/activate_blocked");
    public static final Supplier<GuiElementBuilder> ACTIVATE_OFF_BUTTON = icon32("button/activate_off");
    public static final Supplier<GuiElementBuilder> BUTTON_CLOSE = icon32("button/close");
    public static final Supplier<GuiElementBuilder> BUTTON_DONE = icon32("button/done");
    public static final Supplier<GuiElementBuilder> BUTTON_DONE_BLOCKED = icon32("button/done_blocked");
    public static final Supplier<GuiElementBuilder> LOCKED_SLOT = icon32("locked_slot");
    public static final Progress FLAME = Progress.createVertical("flame", 1, 14, true);
    public static final Progress ICE = Progress.createVertical("ice", 1, 14, true);
    public static final Progress FLAME_OFFSET_RIGHT = Progress.createVertical32Right("flame_offset_right", 9, 22, true);
    public static final Progress ICE_OFFSET_RIGHT = Progress.createVertical32Right("ice_offset_right", 9, 22, true);
    public static final Progress PROGRESS_VERTICAL = Progress.createVertical("progress_vertical", 0, 15, false);
    public static final Progress PROGRESS_HORIZONTAL = Progress.createHorizontal("progress_horizontal", 0, 15, false);
    public static final Progress PROGRESS_HORIZONTAL_OFFSET_RIGHT = Progress.createHorizontal32Right("progress_horizontal_offset_right", 6, 26, false);

    public static final Temperature TEMPERATURE = new Temperature(FLAME, ICE);
    public static final Temperature TEMPERATURE_OFFSET_RIGHT = new Temperature(FLAME_OFFSET_RIGHT, ICE_OFFSET_RIGHT);
    public static final IntFunction<GuiElementBuilder>[] NUMBERS_FLAT = createNumbers("flat/");
    public static final IntFunction<GuiElementBuilder>[] NUMBERS_SHADOW = createNumbers("shadow/");
    public static final char SPACE_1 = UiResourceCreator.space(1);
    public static final char POLYDEX_OFFSET = UiResourceCreator.space(168);
    public static final char POLYDEX_OFFSET_N = UiResourceCreator.space(-168);
    public static final char MIXER_FLUID_OFFSET = UiResourceCreator.space(28 - 8);
    public static final char MIXER_FLUID_OFFSET_N = UiResourceCreator.space(-28 + 8);
    public static final char MECHANICAL_DRAIN_FLUID_OFFSET = UiResourceCreator.space(118 - 8);
    public static final char MECHANICAL_DRAIN_FLUID_OFFSET_N = UiResourceCreator.space(-118 + 8);
    public static final char MECHANICAL_SPOUT_FLUID_OFFSET = UiResourceCreator.space(46 - 8);
    public static final char MECHANICAL_SPOUT_FLUID_OFFSET_N = UiResourceCreator.space(-46 + 8);
    public static final char DRAIN_POLYDEX_FLUID_OFFSET = UiResourceCreator.space(110 + 7);
    public static final char DRAIN_POLYDEX_FLUID_OFFSET_N = UiResourceCreator.space(-110 - 7);
    public static final char SPOUT_POLYDEX_FLUID_OFFSET = UiResourceCreator.space(38 + 7);
    public static final char SPOUT_POLYDEX_FLUID_OFFSET_N = UiResourceCreator.space(-38 - 7);
    public static final char MIXER_POLYDEX_FLUID_OFFSET_1 = UiResourceCreator.space(28 - 8 + 7);
    public static final char MIXER_POLYDEX_FLUID_OFFSET_2 = UiResourceCreator.space(108);
    public static final char MIXER_POLYDEX_FLUID_OFFSET_N = UiResourceCreator.space(-28 + 8 - 7 - 108);

    public static void register() {
        PolydexTextures.register();
    }

    public record Temperature(Progress fire, Progress ice) {
        public GuiElement get(float progress) {
            if (progress < 0) {
                return ice.get(-progress);
            }

            return fire.get(progress);
        }
        public ItemStack getNamed(float progress, Text text) {
            if (progress < 0) {
                return ice.getNamed(-progress, text);
            }

            return fire.getNamed(progress, text);
        }
    }

    public record Progress(GuiElement[] elements, ItemStack[] withTooltip) {
        public GuiElement get(float progress) {
            return elements[Math.min((int) (progress * elements.length), elements.length - 1)];
        }

        public ItemStack getNamed(float progress, Text text) {
            var base = withTooltip[Math.min((int) (progress * withTooltip.length), withTooltip.length - 1)].copy();
            base.set(DataComponentTypes.ITEM_NAME, text);
            return base;
        }

        private static Progress create(int size, IntFunction<GuiElementBuilder> function) {
            var elements = new GuiElement[size + 1];
            var withTooltip = new ItemStack[size + 1];

            elements[0] = EMPTY;
            withTooltip[0] = EMPTY.getItemStack().copy();

            for (var i = 1; i <= size; i++) {
                elements[i] = function.apply(i - 1).hideTooltip().build();
                withTooltip[i] = function.apply(i - 1).asStack();
            }
            return new Progress(elements, withTooltip);
        }

        public static Progress createVertical(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var function = verticalProgress16(path, start, stop, reverse);

            return create(size, function);
        }

        public static Progress createHorizontal(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var function = horizontalProgress16(path, start, stop, reverse);

            return create(size, function);
        }

        public static Progress createHorizontal32(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var function = horizontalProgress32(path, start, stop, reverse);

            return create(size, function);
        }

        public static Progress createHorizontal32Right(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var function = horizontalProgress32Right(path, start, stop, reverse);

            return create(size, function);
        }
        public static Progress createVertical32Right(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var function = verticalProgress32Right(path, start, stop, reverse);

            return create(size, function);
        }
    }

}
