package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.item.FactoryItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;

public interface DyeColorExtra {
    Int2ObjectMap<DyeColor> BY_COLOR = new Int2ObjectOpenHashMap<>();
    int polyfactory$getColor();

    static int getColor(DyeColor color) {
        return ((DyeColorExtra) (Object) color).polyfactory$getColor();
    }

    static int getColor(ItemStack stack) {
        if (stack.isOf(FactoryItems.ARTIFICIAL_DYE)) {
            return ColoredItem.getColor(stack);
        }

        return stack.getItem() instanceof DyeItem dyeItem ? getColor(dyeItem.getColor()) : -1;
    }
}
