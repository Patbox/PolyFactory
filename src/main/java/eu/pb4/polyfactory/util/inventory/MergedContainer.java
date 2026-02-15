package eu.pb4.polyfactory.util.inventory;


import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record MergedContainer(List<Container> containers, int size) implements Container {
    public MergedContainer(Container... containers) {
        this(List.of(containers));
    }
    public MergedContainer(List<Container> containers) {
        this(containers, calculateSize(containers));
    }

    private static int calculateSize(List<Container> containers) {
        int size = 0;
        for (var c : containers) {
            size += c.getContainerSize();
        }
        return size;
    }


    @Override
    public int getContainerSize() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        for (var c : containers) {
            if (!c.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        for (var c : containers) {
            if (slot < c.getContainerSize()) {
                return c.getItem(slot);
            }
            slot -= c.getContainerSize();
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        for (var c : containers) {
            if (slot < c.getContainerSize()) {
                return c.removeItem(slot, amount);
            }
            slot -= c.getContainerSize();
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        for (var c : containers) {
            if (slot < c.getContainerSize()) {
                return c.removeItemNoUpdate(slot);
            }
            slot -= c.getContainerSize();
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        for (var c : containers) {
            if (slot < c.getContainerSize()) {
                c.setItem(slot, stack);
                return;
            }
            slot -= c.getContainerSize();
        }
    }

    @Override
    public void setChanged() {
        for (var c : containers) {
            c.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (var c : containers) {
            c.clearContent();
        }
    }
}
