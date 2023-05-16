package eu.pb4.polyfactory.util.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface RedirectingSidedInventory extends SidedInventory {
    
    SidedInventory getRedirect();

    @Override
    default int[] getAvailableSlots(Direction side) {
        return getRedirect().getAvailableSlots(side);
    }

    @Override
    default boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return getRedirect().canInsert(slot, stack, dir);
    }

    @Override
    default boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return getRedirect().canExtract(slot, stack, dir);
    }

    @Override
    default int size() {
        return getRedirect().size();
    }

    @Override
    default boolean isEmpty() {
        return getRedirect().isEmpty();
    }

    @Override
    default ItemStack getStack(int slot) {
        return getRedirect().getStack(slot);
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        return getRedirect().removeStack(slot, amount);
    }

    @Override
    default ItemStack removeStack(int slot) {
        return getRedirect().removeStack(slot);
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        getRedirect().setStack(slot, stack);
    }

    @Override
    default void markDirty() {
        getRedirect().markDirty();
    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return getRedirect().canPlayerUse(player);
    }

    @Override
    default void clear() {
        getRedirect().clear();
    }
}
