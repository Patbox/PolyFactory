package eu.pb4.polyfactory.util.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface SingleStackInventory extends SidedInventory {
    int[] SLOTS = new int[] { 0 };
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

    @Override
    default int[] getAvailableSlots(Direction side) {
        return SLOTS;
    }

    @Override
    default boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    default boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }
}
