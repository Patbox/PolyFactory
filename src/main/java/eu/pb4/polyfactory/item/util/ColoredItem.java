package eu.pb4.polyfactory.item.util;

import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryColors;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;

import java.util.Locale;

public interface ColoredItem {
    static ItemStack stack(Item input, int count, DyeColor color) {
        return stackCrafting(input, count, DyeColorExtra.getColor(color));
    }
    static ItemStack stack(Item input, int count, int color) {
        var stack = new ItemStack(input, count);
        setColor(stack, color);
        return stack;
    }

    static ItemStack stackCrafting(Item input, int count, int color) {
        var stack = new ItemStack(input, count);
        setColorCrafting(stack, color);
        return stack;
    }

    static String getHexName(int color) {
        return String.format(Locale.ROOT, "#%06X", color);
    }

    int getDefaultColor();

    default int downSampleColor(int color, boolean isVanilla) {
        return color;
    }


    static int getColor(ItemStack stack) {
        if (stack.getItem() instanceof ColoredItem coloredItem) {
            if (stack.hasNbt() && stack.getNbt().contains("color", NbtElement.NUMBER_TYPE)) {
                return stack.getNbt().getInt("color");
            }

            return coloredItem.getDefaultColor();
        }

        return -1;
    }

    static boolean hasColor(ItemStack stack) {
        if (stack.getItem() instanceof ColoredItem) {
            if (stack.hasNbt() && stack.getNbt().contains("color", NbtElement.NUMBER_TYPE)) {
                return true;
            }
        }

        return false;
    }

    static boolean setColor(ItemStack stack, int color) {
        if (stack.getItem() instanceof ColoredItem) {
            stack.getOrCreateNbt().putInt("color", color);
            return true;
        }

        return false;
    }

    static boolean setColorCrafting(ItemStack stack, int color) {
        if (stack.getItem() instanceof ColoredItem coloredItem) {
            stack.getOrCreateNbt().putInt("color", coloredItem.downSampleColor(color, DyeColorExtra.BY_COLOR.containsKey(color)));
            return true;
        }

        return false;
    }

    static Text getColorName(int color) {
        if (color == FactoryColors.YTTR_TEAL) {
            return Text.translatable("color.polyfactory.teal");
        }

        DyeColor dyeColor = DyeColorExtra.BY_COLOR.get(color);

        return dyeColor == null ? Text.translatable("item.minecraft.firework_star.custom_color") : Text.translatable("item.minecraft.firework_star." + dyeColor.getName());
    }
}
