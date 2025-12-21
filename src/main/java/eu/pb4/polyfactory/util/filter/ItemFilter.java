package eu.pb4.polyfactory.util.filter;

import net.minecraft.world.item.ItemStack;

public interface ItemFilter {
    boolean test(ItemStack stack);
}
