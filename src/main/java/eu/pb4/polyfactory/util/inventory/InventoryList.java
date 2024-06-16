package eu.pb4.polyfactory.util.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import java.util.AbstractList;

public class InventoryList extends AbstractList<ItemStack> {
    private final Inventory backing;
    private final int start;
    private final int end;
    private final int size;

    public InventoryList(Inventory inventory, int start, int end) {
        this.backing = inventory;
        this.start = start;
        this.end = end;
        this.size = end - start;
    }

    @Override
    public ItemStack get(int index) {
        return this.backing.getStack(index(index));
    }

    @Override
    public int size() {
        return this.backing.size();
    }

    @Override
    public ItemStack set(int index, ItemStack element) {
        index = index(index);
        var old = this.backing.getStack(index);
        this.backing.setStack(index,element);
        return old;
    }

    private int index(int index) {
        return index;
    }
}
