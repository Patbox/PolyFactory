package eu.pb4.polyfactory.ui;

import eu.pb4.sgui.api.elements.GuiElement;
import net.minecraft.text.Text;

import java.util.function.Function;

import static eu.pb4.polyfactory.ui.UiResourceCreator.*;

public class GuiTextures {
    public static final Function<Text, Text> GRINDER = background("grinder");
    public static final Function<Text, Text> PRESS = background("press");
    public static final Function<Text, Text> MIXER = background("mixer");
    public static final Function<Text, Text> STEAM_ENGINE = background("steam_engine");
    public static final Function<Text, Text> CENTER_SLOT_GENERIC = background("center_slot");
    public static final GuiElement EMPTY = icon16("empty").get().build();
    public static final Progress FLAME = Progress.createVertical("flame", 1, 14, true);
    public static final Progress FLAME_OFFSET_RIGHT = Progress.createVertical32Right("flame_offset_right", 9, 22, true);
    public static final Progress PROGRESS_VERTICAL = Progress.createVertical("progress_vertical", 0, 15, false);
    //public static final Progress PROGRESS_HORIZONTAL = Progress.createHorizontal("progress_horizontal", 0, 15, false);
    public static final Progress PROGRESS_HORIZONTAL_OFFSET_RIGHT = Progress.createHorizontal32Right("progress_horizontal_offset_right", 6, 26, false);

    public static void register() {
    }

    public record Progress(GuiElement[] elements) {

        public GuiElement get(float progress) {
            return elements[Math.min((int) (progress * elements.length), elements.length - 1)];
        }

        public static Progress createVertical(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var elements = new GuiElement[size + 1];
            var function = verticalProgress16(path, start, stop, reverse);

            elements[0] = EMPTY;

            for (var i = 1; i <= size; i++) {
                elements[i] = function.apply(i - 1).build();
            }
            return new Progress(elements);
        }

        public static Progress createHorizontal(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var elements = new GuiElement[size + 1];
            var function = horizontalProgress16(path, start, stop, reverse);

            elements[0] = EMPTY;

            for (var i = 1; i <= size; i++) {
                elements[i] = function.apply(i - 1).build();
            }
            return new Progress(elements);
        }

        public static Progress createHorizontal32(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var elements = new GuiElement[size + 1];
            var function = horizontalProgress32(path, start, stop, reverse);

            elements[0] = EMPTY;

            for (var i = 1; i <= size; i++) {
                elements[i] = function.apply(i - 1).build();
            }
            return new Progress(elements);
        }

        public static Progress createHorizontal32Right(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var elements = new GuiElement[size + 1];
            var function = horizontalProgress32Right(path, start, stop, reverse);

            elements[0] = EMPTY;

            for (var i = 1; i <= size; i++) {
                elements[i] = function.apply(i - 1).build();
            }
            return new Progress(elements);
        }
        public static Progress createVertical32Right(String path, int start, int stop, boolean reverse) {
            var size = stop - start;
            var elements = new GuiElement[size + 1];
            var function = verticalProgress32Right(path, start, stop, reverse);

            elements[0] = EMPTY;

            for (var i = 1; i <= size; i++) {
                elements[i] = function.apply(i - 1).build();
            }
            return new Progress(elements);
        }
    }
}
