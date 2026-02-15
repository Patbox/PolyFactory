package eu.pb4.polyfactory.util.inventory;

import java.util.AbstractList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class ContainerList extends AbstractList<ItemStack> {
    public final Container backing;
    private final int start;
    private final int end;
    private final int size;

    public ContainerList(Container inventory, int start, int end) {
        this.backing = inventory;
        this.start = start;
        this.end = end;
        this.size = end - start;
    }

    @Override
    public ItemStack get(int index) {
        return this.backing.getItem(index(index));
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public ItemStack set(int index, ItemStack element) {
        index = index(index);
        var old = this.backing.getItem(index);
        this.backing.setItem(index,element);
        return old;
    }

    private int index(int index) {
        return index + start;
    }
}
