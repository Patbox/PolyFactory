package eu.pb4.polyfactory.util.filter;

import net.minecraft.item.ItemStack;

public interface ItemFilter {
    boolean test(ItemStack stack);
}
