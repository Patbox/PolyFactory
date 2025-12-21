package eu.pb4.polyfactory.util.inventory;

import java.util.function.IntConsumer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface MinimalContainer extends Container {
    static MinimalContainer create(NonNullList<ItemStack> items, Runnable markDirty) {
        return new MinimalContainer() {
            @Override
            public NonNullList<ItemStack> getStacks() {
                return items;
            }

            @Override
            public void setChanged() {
                markDirty.run();
            }
        };
    }

    static MinimalContainer create(NonNullList<ItemStack> items, Runnable markDirty, IntConsumer markSlotDirty) {
        return new MinimalContainer() {
            @Override
            public NonNullList<ItemStack> getStacks() {
                return items;
            }

            @Override
            public void setChanged() {
                markDirty.run();
            }

            @Override
            public void markSlotDirty(int slot) {
                markSlotDirty.accept(slot);
            }
        };
    }

    static MinimalContainer createMaxOne(NonNullList<ItemStack> items, Runnable markDirty, IntConsumer markSlotDirty) {
        return new MinimalContainer() {
            @Override
            public NonNullList<ItemStack> getStacks() {
                return items;
            }

            @Override
            public void setChanged() {
                markDirty.run();
            }

            @Override
            public void markSlotDirty(int slot) {
                markSlotDirty.accept(slot);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
    }

    NonNullList<ItemStack> getStacks();

    @Override
    default int getContainerSize() {
        return getStacks().size();
    }

    @Override
    default boolean isEmpty() {
        var var1 = this.getStacks().iterator();

        ItemStack itemStack;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            itemStack = (ItemStack)var1.next();
        } while(itemStack.isEmpty());

        return false;
    }

    @Override
    default ItemStack getItem(int slot) {
        return getStacks().get(slot);
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        ItemStack itemStack = ContainerHelper.removeItem(this.getStacks(), slot, amount);
        if (!itemStack.isEmpty()) {
            this.setChanged();
            this.markSlotDirty(slot);
        }

        return itemStack;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        var x = ContainerHelper.takeItem(this.getStacks(), slot);
        this.setChanged();
        this.markSlotDirty(slot);
        return x;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        this.getStacks().set(slot, stack);
        this.setChanged();
        this.markSlotDirty(slot);
    }

    @Override
    default boolean stillValid(Player player) {
        return true;
    }

    @Override
    default void clearContent() {
        this.getStacks().clear();
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.markSlotDirty(i);
        }
        this.setChanged();
    }

    default void markSlotDirty(int slot) {};
}
