package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.item.FactoryItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

public interface DyeColorExtra {
    Int2ObjectMap<DyeColor> BY_COLOR = new Int2ObjectOpenHashMap<>();

    static boolean hasLang(int color) {
        return FactoryColors.YTTR_TEAL == color || BY_COLOR.get(color) != null;
    }

    int polyfactory$getColor();

    static int getColor(DyeColor color) {
        return ((DyeColorExtra) (Object) color).polyfactory$getColor();
    }

    static int getColor(ItemStack stack) {
        if (stack.is(FactoryItems.ARTIFICIAL_DYE)) {
            return ColoredItem.getColor(stack);
        }

        return stack.getItem() instanceof DyeItem dyeItem ? getColor(dyeItem.getDyeColor()) : -1;
    }
}
