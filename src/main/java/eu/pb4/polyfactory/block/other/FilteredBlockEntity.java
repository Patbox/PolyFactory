package eu.pb4.polyfactory.block.other;

import net.minecraft.world.item.ItemStack;

public interface FilteredBlockEntity {
    ItemStack polyfactory$getFilter();
    void polyfactory$setFilter(ItemStack itemStack);
    boolean polyfactory$matchesFilter(ItemStack itemStack);
}
