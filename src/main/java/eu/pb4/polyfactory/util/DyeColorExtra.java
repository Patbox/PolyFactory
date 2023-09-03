package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.item.ArtificialDyeItem;
import eu.pb4.polyfactory.item.FactoryItems;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DyeColor;

public interface DyeColorExtra {
    int polyfactory$getColor();

    static int getColor(DyeColor color) {
        return ((DyeColorExtra) (Object) color).polyfactory$getColor();
    }

    static int getColor(ItemStack stack) {
        if (stack.isOf(FactoryItems.ARTIFICIAL_DYE)) {
            return ArtificialDyeItem.getColor(stack);
        }

        return stack.getItem() instanceof DyeItem dyeItem ? getColor(dyeItem.getColor()) : -1;
    }
}
