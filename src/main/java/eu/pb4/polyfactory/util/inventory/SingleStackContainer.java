package eu.pb4.polyfactory.util.inventory;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface SingleStackContainer extends WorldlyContainer {
    int[] SLOTS = new int[] { 0 };
    ItemStack getStack();
    void setStack(ItemStack stack);

    @Override
    default int getContainerSize() {
        return 1;
    }

    @Override
    default boolean isEmpty() {
        return getStack().isEmpty();
    }

    @Override
    default ItemStack getItem(int slot) {
        return getStack();
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        var stack = getStack();
        amount = Math.min(stack.getCount(), amount);

        var copy = stack.copyWithCount(amount);
        stack.shrink(amount);

        if (stack.isEmpty()) {
            setStack(ItemStack.EMPTY);
        }

        return copy;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        var stack = getStack();
        setStack(ItemStack.EMPTY);
        return stack;
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        setStack(stack);
    }

    @Override
    default void setChanged() {

    }

    @Override
    default boolean stillValid(Player player) {
        return false;
    }

    @Override
    default void clearContent() {
        setStack(ItemStack.EMPTY);
    }

    @Override
    default int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    default boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    default boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }
}
