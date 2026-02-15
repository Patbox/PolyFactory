package eu.pb4.polyfactory.util.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.AbstractList;

public class SubContainer implements Container {
    private final Container backing;
    private final int start;
    private final int end;
    private final int size;

    public SubContainer(Container inventory, int start) {
        this(inventory, start, inventory.getContainerSize());
    }
    public SubContainer(Container inventory, int start, int end) {
        this.backing = inventory;
        this.start = start;
        this.end = end;
        this.size = end - start;
    }

    private int index(int index) {
        return index + start;
    }

    @Override
    public int getContainerSize() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (var i = this.start; i < this.end; i++) {
            if (!this.backing.getItem(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.backing.getItem(index(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.backing.removeItem(index(slot), amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.backing.removeItemNoUpdate(index(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.backing.setItem(index(slot), stack);
    }

    @Override
    public void setChanged() {
        this.backing.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.backing.stillValid(player);
    }

    @Override
    public void clearContent() {
        for (var i = this.start; i < this.end; i++) {
            this.backing.setItem(i, ItemStack.EMPTY);
        }
    }
}
