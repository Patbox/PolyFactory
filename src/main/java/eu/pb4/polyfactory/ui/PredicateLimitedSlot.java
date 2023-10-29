package eu.pb4.polyfactory.ui;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.slot.Slot;

import java.util.function.Predicate;

public class PredicateLimitedSlot extends Slot {
    private final Predicate<ItemStack> predicate;

    public PredicateLimitedSlot(Inventory inventory, int index, Predicate<ItemStack> predicate) {
        super(inventory, index, index, 0);
        this.predicate = predicate;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return predicate.test(stack);
    }
}
