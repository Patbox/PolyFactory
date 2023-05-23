package eu.pb4.polyfactory.util.movingitem;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public interface InventoryContainerHolderProvider extends Inventory {
    ContainerHolder getContainerHolder(int slot);

    @Override
    default ItemStack getStack(int slot) {
        var x = getContainerHolder(slot).getContainer();
        if (x != null) {
            return x.get();
        }
        return ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        var h = getContainerHolder(slot);
        var x = h.getContainer();
        if (x != null && !x.get().isEmpty()) {
            var a = Math.min(x.get().getCount(), amount);
            var o = x.get().copyWithCount(a);
            x.get().decrement(a);
            if (x.get().isEmpty()) {
                h.clearContainer();
            }
            return o;
        }
        return ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeStack(int slot) {
        var h = getContainerHolder(slot);
        var x = h.getContainer();
        if (x != null) {
            h.clearContainer();
            return x.get();
        }
        return ItemStack.EMPTY;
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        var h = getContainerHolder(slot);
        if (stack.isEmpty()) {
            h.clearContainer();
        } else {
            h.setContainer(new MovingItem(stack));
        }
    }
}
