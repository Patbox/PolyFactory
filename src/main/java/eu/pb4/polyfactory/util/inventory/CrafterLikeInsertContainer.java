package eu.pb4.polyfactory.util.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface CrafterLikeInsertContainer extends CustomInsertContainer {
    @Override
    default int insertStack(ItemStack itemStack, @Nullable Direction direction) {
        var init = itemStack.getCount();
        while (true) {
            if (itemStack.isEmpty()) {
                return init;
            }
            var slot = this.getLeastPopulatedInputSlot(itemStack);
            if (slot == -1) {
                return init - itemStack.getCount();
            }

            var current = this.getItem(slot);
            if (current.isEmpty()) {
                this.setItem(slot, itemStack.copyWithCount(1));
                itemStack.shrink(1);
            } else {
                current.grow(1);
                itemStack.shrink(1);
            }
        }
    }

    @Override
    default int insertStackSlots(ItemStack itemStack, @Nullable Direction direction, IntList slots) {
        var init = itemStack.getCount();
        while (true) {
            if (itemStack.isEmpty()) {
                return init;
            }
            var slot = this.getLeastPopulatedInputSlot(itemStack, slots);
            if (slot == -1) {
                return init - itemStack.getCount();
            }

            var current = this.getItem(slot);
            if (current.isEmpty()) {
                this.setItem(slot, itemStack.copyWithCount(1));
                itemStack.shrink(1);
            } else {
                current.grow(1);
                itemStack.shrink(1);
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
            var cur = this.getItem(i);

            if (cur.isEmpty()) {
                return i;
            }

            if (ItemStack.isSameItemSameComponents(cur, stack)) {
                if (count > cur.getCount() && cur.getCount() < cur.getMaxStackSize()) {
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
            var cur = this.getItem(i);

            if (cur.isEmpty()) {
                return i;
            }

            if (ItemStack.isSameItemSameComponents(cur, stack)) {
                if (count > cur.getCount() && cur.getCount() < cur.getMaxStackSize()) {
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
