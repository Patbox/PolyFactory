package eu.pb4.polyfactory.ui;

import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TagLimitedSlot extends Slot {
    private final TagKey<Item> tagKey;

    public TagLimitedSlot(Container inventory, int index, TagKey<Item> tagKey) {
        super(inventory, index, index, 0);
        this.tagKey = tagKey;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(this.tagKey);
    }
}
