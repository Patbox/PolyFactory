package eu.pb4.polyfactory.util.inventory;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import java.util.AbstractList;

public class InventoryList extends AbstractList<ItemStack> {
    private final Inventory backing;

    public InventoryList(Inventory inventory) {
        this.backing = inventory;
    }

    @Override
    public ItemStack get(int index) {
        return this.backing.getStack(index);
    }

    @Override
    public int size() {
        return this.backing.size();
    }

    @Override
    public ItemStack set(int index, ItemStack element) {
        var old = this.backing.getStack(index);
        this.backing.setStack(index,element);
        return old;
    }
}
