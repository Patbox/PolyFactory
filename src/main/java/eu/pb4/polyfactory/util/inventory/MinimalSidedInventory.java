package eu.pb4.polyfactory.util.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public interface MinimalSidedInventory extends SidedInventory {
    DefaultedList<ItemStack> getStacks();

    @Override
    default int size() {
        return getStacks().size();
    }

    @Override
    default boolean isEmpty() {
        Iterator var1 = this.getStacks().iterator();

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
    default ItemStack getStack(int slot) {
        return getStacks().get(slot);
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        ItemStack itemStack = Inventories.splitStack(this.getStacks(), slot, amount);
        if (!itemStack.isEmpty()) {
            this.markDirty();
            this.markSlotDirty(slot);
        }

        return itemStack;
    }

    @Override
    default ItemStack removeStack(int slot) {
        var x = Inventories.removeStack(this.getStacks(), slot);
        this.markDirty();
        this.markSlotDirty(slot);
        return x;
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        this.getStacks().set(slot, stack);
        this.markDirty();
        this.markSlotDirty(slot);
    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    default void clear() {
        this.getStacks().clear();
        for (int i = 0; i < this.size(); i++) {
            this.markSlotDirty(i);
        }
        this.markDirty();
    }

    default void markSlotDirty(int slot) {};
}
