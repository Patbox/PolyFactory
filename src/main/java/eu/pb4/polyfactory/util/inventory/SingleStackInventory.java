package eu.pb4.polyfactory.util.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;

public interface SingleStackInventory extends Inventory {
    ItemStack getStack();
    void setStack(ItemStack stack);

    @Override
    default int size() {
        return 1;
    }

    @Override
    default boolean isEmpty() {
        return getStack().isEmpty();
    }

    @Override
    default ItemStack getStack(int slot) {
        return getStack();
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        var stack = getStack();
        amount = Math.min(stack.getCount(), amount);

        var copy = stack.copyWithCount(amount);
        stack.decrement(amount);

        if (stack.isEmpty()) {
            setStack(ItemStack.EMPTY);
        }

        return copy;
    }

    @Override
    default ItemStack removeStack(int slot) {
        var stack = getStack();
        setStack(ItemStack.EMPTY);
        return stack;
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        setStack(stack);
    }

    @Override
    default void markDirty() {

    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    @Override
    default void clear() {
        setStack(ItemStack.EMPTY);
    }
}
