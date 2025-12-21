package eu.pb4.polyfactory.util.movingitem;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface MovingItemContainerHolderProvider extends Container {

    MovingItemContainerHolder getContainerHolder(int slot);

    @Override
    default ItemStack getItem(int slot) {
        var x = getContainerHolder(slot).getContainer();
        if (x != null) {
            return x.get();
        }
        return ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        var h = getContainerHolder(slot);
        var x = h.getContainer();
        if (x != null && !x.get().isEmpty()) {
            var a = Math.min(x.get().getCount(), amount);
            var o = x.get().copyWithCount(a);
            x.get().shrink(a);
            if (x.get().isEmpty()) {
                h.clearContainer();
            }
            return o;
        }
        this.setChanged();
        return ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        var h = getContainerHolder(slot);
        var x = h.getContainer();
        if (x != null) {
            h.clearContainer();
            this.setChanged();
            return x.get();
        }
        return ItemStack.EMPTY;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        var h = getContainerHolder(slot);
        if (stack.isEmpty()) {
            h.clearContainer();
        } else if (h.getContainer() != null) {
            h.getContainer().set(stack);
        } else {
            h.setContainer(new MovingItem(stack));
        }
        this.setChanged();
    }
}
