package eu.pb4.polyfactory.ui;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.slot.Slot;

public class TagLimitedSlot extends Slot {
    private final TagKey<Item> tagKey;

    public TagLimitedSlot(Inventory inventory, int index, TagKey<Item> tagKey) {
        super(inventory, index, index, 0);
        this.tagKey = tagKey;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return stack.isIn(this.tagKey);
    }
}
