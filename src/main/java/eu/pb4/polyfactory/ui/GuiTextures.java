package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.polydex.PolydexTextures;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static eu.pb4.polyfactory.ModInit.id;
import static eu.pb4.polyfactory.ui.UiResourceCreator.*;

public class GuiTextures {
    public static final Function<Component, Component> CRAFTER = background("crafter");
    public static final Function<Component, Component> GRINDER = background("grinder");
    public static final Function<Component, Component> PRESS = background("press");
    public static final Function<Component, Component> MIXER = background("mixer");
    public static final Function<Component, Component> MECHANICAL_DRAIN = background("mechanical_drain");
    public static final Function<Component, Component> MECHANICAL_SPOUT = background("mechanical_spout");
    public static final Function<Component, Component> MECHANICAL_SPOUT_NO_CONN = background("mechanical_spout_noconn");
    public static final Function<Component, Component> STEAM_ENGINE = background("steam_engine");
    public static final Function<Component, Component> SMELTERY = background("smeltery");
    public static final Function<Component, Component> PRIMITIVE_SMELTERY = background("primitive_smeltery");
    public static final Function<Component, Component> CENTER_SLOT_GENERIC = background("center_slot");
    public static final Function<Component, Component> DEEP_STORAGE_CONTAINER = background("deep_storage_container");
    public static final Function<Component, Component> FILL3 = background("fill3");
    public static final Function<Component, Component> BLUEPRINT_WORKBENCH = background("blueprint_workbench");
    public static final Function<Component, Component> SLOT_AWARE_FUNNEL = background("slot_aware_funnel");
    public static final Function<Component, Component> ITEM_FILTER = background("item_filter");
    public static final Function<Component, Component> INPUT = backgroundAnvil("input");
    public static final Function<Component, Component> TEXT_INPUT = backgroundAnvil("text_input");
    public static final Function<Component, Component> DATA_EXTRACTOR = backgroundAnvil("data_extractor");
    public static final Supplier<GuiElementBuilder> EMPTY_BUILDER = icon16("empty");
    public static final GuiElement EMPTY = EMPTY_BUILDER.get().hideTooltip().build();
    public static final Supplier<GuiElementBuilder> POLYDEX_BUTTON = icon32("polydex");
    public static final Supplier<GuiElementBuilder> PLUS_BUTTON = icon32("button/plus");
    public static final Supplier<GuiElementBuilder> MINUS_BUTTON = icon32("button/minus");
    public static final Supplier<GuiElementBuilder> ACTIVATE_BUTTON = icon32("button/activate");
    public static final Supplier<GuiElementBuilder> ACTIVATE_BLOCKED_BUTTON = icon32("button/activate_blocked");
    public static final Supplier<GuiElementBuilder> ACTIVATE_OFF_BUTTON = icon32("button/activate_off");

    public static final Supplier<GuiElementBuilder> BUTTON_ACTIVE_ALWAYS = icon32("button/active/always");
    public static final Supplier<GuiElementBuilder> BUTTON_ACTIVE_POWERED = icon32("button/active/powered");
    public static final Supplier<GuiElementBuilder> BUTTON_ACTIVE_NOT_POWERED = icon32("button/active/not_powered");

    public static final Supplier<GuiElementBuilder> BUTTON_CLOSE = icon32("button/close");
    public static final Supplier<GuiElementBuilder> BUTTON_DONE = icon32("button/done");
    public static final Supplier<GuiElementBuilder> BUTTON_DONE_BLOCKED = icon32("button/done_blocked");
    public static final Supplier<GuiElementBuilder> BUTTON_ITEM_FILTER_TYPE_ONLY = icon32("button/item_filter/type_only");
    public static final Supplier<GuiElementBuilder> BUTTON_ITEM_FILTER_STRICT = icon32("button/item_filter/strict");
    public static final Supplier<GuiElementBuilder> LOCKED_SLOT = icon32("locked_slot");

    public static final Supplier<GuiElementBuilder> NUMBERED_BUTTONS_DISABLED = icon16("numbered_buttons/disabled");
    public static final Supplier<GuiElementBuilder> NUMBERED_BUTTONS_OTHER = icon16("numbered_buttons/other");
    @SuppressWarnings("unchecked")
    public static final Supplier<GuiElementBuilder>[] NUMBERED_BUTTONS = Util.make(new Supplier[100], (arr) -> {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = icon16("numbered_buttons/num_" + i);
        }
    });

    public static final ItemStack ITEM_FILTER_BLOCKED = icon16("item_filter_blocked").get().asStack();

    public static final ItemStack LEFT_SHIFTED_3_BARS = new GuiElementBuilder(Items.TRIAL_KEY).hideTooltip().model(id("-/sgui/left_shifted_3_bars")).asStack();
    public static final ItemStack DEEP_STORAGE_UNIT_SELECTED = new GuiElementBuilder(Items.TRIAL_KEY).hideTooltip().model(id("-/sgui/deep_storage_unit_selected")).asStack();
    public static final Progress FLAME = Progress.createVertical("flame", 1, 14, true);
    public static final Progress ICE = Progress.createVertical("ice", 1, 14, true);
    public static final Progress FLAME_OFFSET_RIGHT = Progress.createVertical32Right("flame_offset_right", 9, 22, true);
    public static final Progress ICE_OFFSET_RIGHT = Progress.createVertical32Right("ice_offset_right", 9, 22, true);
    public static final Progress PROGRESS_VERTICAL = Progress.createVertical("progress_vertical", 0, 15, false);
    public static final Progress PROGRESS_HORIZONTAL = Progress.createHorizontal("progress_horizontal", 0, 15, false);
    public static final Progress PROGRESS_HORIZONTAL_OFFSET_RIGHT = Progress.createHorizontal32Right("progress_horizontal_offset_right", 6, 26, false);

    public static final Temperature TEMPERATURE = new Temperature(FLAME, ICE);
    public static final Temperature TEMPERATURE_OFFSET_RIGHT = new Temperature(FLAME_OFFSET_RIGHT, ICE_OFFSET_RIGHT);
    public static final IntFunction<GuiElementBuilder>[] NUMBERS_FLAT_24 = createNumbers(8 * 3, false, 0);
    public static final IntFunction<GuiElementBuilder>[] NUMBERS_SHADOW_8 = createNumbers(8, true, 0);
    public static final char SPACE_1 = UiResourceCreator.space(1);
    public static final char SPACE_5 = UiResourceCreator.space(5);
    public static final char SPACE_10 = UiResourceCreator.space(10);
    public static final char SPACE_20 = UiResourceCreator.space(20);
    public static final char SPACE_50 = UiResourceCreator.space(50);
    public static final char SPACE_100 = UiResourceCreator.space(100);
    public static final char BLUEPRINT_WORKSTATION_EXTRA_OFFSET = UiResourceCreator.space(-29 + 8);
    public static final char POLYDEX_OFFSET = UiResourceCreator.space(168);
    public static final char POLYDEX_OFFSET_N = UiResourceCreator.space(-168);
    public static final char MIXER_FLUID_OFFSET = UiResourceCreator.space(28 - 8);
    public static final char MIXER_FLUID_OFFSET_N = UiResourceCreator.space(-28 + 8);
    public static final char MECHANICAL_DRAIN_FLUID_OFFSET = UiResourceCreator.space(118 - 8);
    public static final char MECHANICAL_DRAIN_FLUID_OFFSET_N = UiResourceCreator.space(-118 + 8);
    public static final char MECHANICAL_SPOUT_FLUID_OFFSET = UiResourceCreator.space(46 - 8);
    public static final char MECHANICAL_SPOUT_FLUID_OFFSET_N = UiResourceCreator.space(-46 + 8);
    public static final char SMELTERY_FLUID_OFFSET = UiResourceCreator.space(99 - 8);
    public static final char SMELTERY_FLUID_OFFSET_N = UiResourceCreator.space(-99 + 8);
    public static final char DRAIN_POLYDEX_FLUID_OFFSET = UiResourceCreator.space(110 + 7);
    public static final char DRAIN_POLYDEX_FLUID_OFFSET_N = UiResourceCreator.space(-110 - 7);
    public static final char SPOUT_POLYDEX_FLUID_OFFSET = UiResourceCreator.space(38 + 7);
    public static final char SPOUT_POLYDEX_FLUID_OFFSET_N = UiResourceCreator.space(-38 - 7);
    public static final char MIXER_POLYDEX_FLUID_OFFSET_1 = UiResourceCreator.space(28 - 8 + 7);
    public static final char MIXER_POLYDEX_FLUID_OFFSET_2 = UiResourceCreator.space(108);
    public static final char MIXER_POLYDEX_FLUID_OFFSET_N = UiResourceCreator.space(-28 + 8 - 7 - 108);
    public static final char SMELTERY_POLYDEX_FLUID_OFFSET = UiResourceCreator.space(91 + 7);
    public static final char SMELTERY_POLYDEX_FLUID_OFFSET_N = UiResourceCreator.space(-91 - 7);
    public static final char PRESS_POLYDEX_FLUID_OFFSET = UiResourceCreator.space(107 - 8);
    public static final char PRESS_POLYDEX_FLUID_OFFSET_N = UiResourceCreator.space(-107 + 8);
    public static final char NEGATIVE_SPACE_1 = UiResourceCreator.space(-1);
    public static final char NEGATIVE_SPACE_1024 = UiResourceCreator.space(-1024);
    public static final char NEGATIVE_SPACE_100 = UiResourceCreator.space(-100);
    public static final char NEGATIVE_SPACE_50 = UiResourceCreator.space(-50);
    public static final char NEGATIVE_SPACE_20 = UiResourceCreator.space(-20);
    public static final char NEGATIVE_SPACE_10 = UiResourceCreator.space(-10);

    public static final char INVIS_LINE_RAW = UiResourceCreator.font(id("sgui/invis_line"), 7, 8);
    public static final char INVIS_FULL_1024_RAW = UiResourceCreator.font(id("sgui/invis_full"), 512, 1024);
    public static final char INVIS_FULL_100_RAW = UiResourceCreator.font(id("sgui/invis_full"), 50, 100);
    public static final char INVIS_FULL_50_RAW = UiResourceCreator.font(id("sgui/invis_full"), 25, 50);
    public static final char INVIS_FULL_20_RAW = UiResourceCreator.font(id("sgui/invis_full"), 10, 20);
    public static final char INVIS_FULL_10_RAW = UiResourceCreator.font(id("sgui/invis_full"), 5, 10);

    public static void register() {
        PolydexTextures.register();
        Progress.createHorizontal("generic_bar", 1, 15, false, 0);
        Progress.createHorizontal("generic_bar", 1, 15, false, -18);
        Progress.createHorizontal("generic_bar", 1, 15, false, -18 * 2);
        Progress.createHorizontal("generic_bar", 1, 15, false, -18 * 3);
        UiResourceCreator.icon16Offset("generic_bar_background", 0);
        UiResourceCreator.icon16Offset("generic_bar_background", -18);
        UiResourceCreator.icon16Offset("generic_bar_background", -18 * 2);
        UiResourceCreator.icon16Offset("generic_bar_background", -18 * 3);
    }

    public record Temperature(Progress fire, Progress ice) {
        public GuiElement get(float progress) {
            if (progress < 0) {
                return ice.get(-progress);
            }

            return fire.get(progress);
        }
        public ItemStack getNamed(float progress, Component text) {
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

        public GuiElement getCeil(float progress) {
            return elements[Math.min((int) Math.ceil(progress * elements.length), elements.length - 1)];
        }

        public ItemStack getNamed(float progress, Component text) {
            var base = withTooltip[Math.min((int) (progress * withTooltip.length), withTooltip.length - 1)].copy();
            base.set(DataComponents.ITEM_NAME, text);
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
            var function = horizontalProgress16(path, start, stop, reverse, 0);

            return create(size, function);
        }

        public static Progress createHorizontal(String path, int start, int stop, boolean reverse, int offset) {
            var size = stop - start;
            var function = horizontalProgress16(path, start, stop, reverse, offset);

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
