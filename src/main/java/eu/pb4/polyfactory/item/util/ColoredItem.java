package eu.pb4.polyfactory.item.util;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryColors;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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

    default int downSampleColor(int color) {
        return downSampleColor(color, DyeColorExtra.BY_COLOR.containsKey(color));
    }

    default int downSampleColor(int color, boolean isVanilla) {
        return color;
    }


    static int getColor(ItemStack stack) {
        if (stack.getItem() instanceof ColoredItem coloredItem) {
            return stack.getOrDefault(FactoryDataComponents.COLOR, coloredItem.getDefaultColor());
        }

        return -1;
    }

    static boolean hasColor(ItemStack stack) {
        if (stack.getItem() instanceof ColoredItem) {
            return stack.has(FactoryDataComponents.COLOR);
        }

        return false;
    }

    static boolean setColor(ItemStack stack, int color) {
        if (stack.getItem() instanceof ColoredItem) {
            stack.set(FactoryDataComponents.COLOR, color);
            return true;
        }

        return false;
    }

    static boolean setColorCrafting(ItemStack stack, int color) {
        if (stack.getItem() instanceof ColoredItem coloredItem) {
            stack.set(FactoryDataComponents.COLOR, coloredItem.downSampleColor(color));
            return true;
        }

        return false;
    }

    static Component getColorName(int color) {
        if (color == FactoryColors.YTTR_TEAL) {
            return Component.translatable("color.polyfactory.teal");
        }

        DyeColor dyeColor = DyeColorExtra.BY_COLOR.get(color);

        return dyeColor == null ? Component.translatable("item.minecraft.firework_star.custom_color") : Component.translatable("item.minecraft.firework_star." + dyeColor.getSerializedName());
    }
}
