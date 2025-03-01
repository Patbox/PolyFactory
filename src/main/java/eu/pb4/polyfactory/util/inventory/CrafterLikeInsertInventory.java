package eu.pb4.polyfactory.util.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public interface CrafterLikeInsertInventory extends CustomInsertInventory {
    @Override
    default int insertStack(ItemStack itemStack, Direction direction) {
        var init = itemStack.getCount();
        while (true) {
            if (itemStack.isEmpty()) {
                return init;
            }
            var slot = this.getLeastPopulatedInputSlot(itemStack);
            if (slot == -1) {
                return init - itemStack.getCount();
            }

            var current = this.getStack(slot);
            if (current.isEmpty()) {
                this.setStack(slot, itemStack.copyWithCount(1));
                itemStack.decrement(1);
            } else {
                current.increment(1);
                itemStack.decrement(1);
            }
        }
    }

    @Override
    default int insertStackSlots(ItemStack itemStack, Direction direction, IntList slots) {
        var init = itemStack.getCount();
        while (true) {
            if (itemStack.isEmpty()) {
                return init;
            }
            var slot = this.getLeastPopulatedInputSlot(itemStack, slots);
            if (slot == -1) {
                return init - itemStack.getCount();
            }

            var current = this.getStack(slot);
            if (current.isEmpty()) {
                this.setStack(slot, itemStack.copyWithCount(1));
                itemStack.decrement(1);
            } else {
                current.increment(1);
                itemStack.decrement(1);
            }
        }
    }

    default int getLeastPopulatedInputSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        int slot = -1;
        int count = 9999;

        for (int i = 0; i < this.inputSize(); i++) {
            if (this.isSlotLocked(i)) {
                continue;
            }
            var cur = this.getStack(i);

            if (cur.isEmpty()) {
                return i;
            }

            if (ItemStack.areItemsAndComponentsEqual(cur, stack)) {
                if (count > cur.getCount() && cur.getCount() < cur.getMaxCount()) {
                    count = cur.getCount();
                    slot = i;
                }
            }
        }

        return slot;
    }

    default int getLeastPopulatedInputSlot(ItemStack stack, IntList slots) {
        if (stack.isEmpty()) {
            return -1;
        }
        int slot = -1;
        int count = 9999;

        for (int i : slots) {
            if (i >= this.inputSize() || this.isSlotLocked(i)) {
                continue;
            }
            var cur = this.getStack(i);

            if (cur.isEmpty()) {
                return i;
            }

            if (ItemStack.areItemsAndComponentsEqual(cur, stack)) {
                if (count > cur.getCount() && cur.getCount() < cur.getMaxCount()) {
                    count = cur.getCount();
                    slot = i;
                }
            }
        }

        return slot;
    }

    default boolean isSlotLocked(int i) {
        return false;
    }
    int inputSize();
}
