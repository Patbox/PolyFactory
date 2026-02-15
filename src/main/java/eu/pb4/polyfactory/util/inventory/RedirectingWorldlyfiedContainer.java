package eu.pb4.polyfactory.util.inventory;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface RedirectingWorldlyfiedContainer extends WorldlyContainer {
    
    Container getRedirect();

    @Override
    default int getContainerSize() {
        return getRedirect().getContainerSize();
    }

    @Override
    default boolean isEmpty() {
        return getRedirect().isEmpty();
    }

    @Override
    default ItemStack getItem(int slot) {
        return getRedirect().getItem(slot);
    }

    @Override
    default ItemStack removeItem(int slot, int amount) {
        return getRedirect().removeItem(slot, amount);
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return getRedirect().removeItemNoUpdate(slot);
    }

    @Override
    default void setItem(int slot, ItemStack stack) {
        getRedirect().setItem(slot, stack);
    }

    @Override
    default void setChanged() {
        getRedirect().setChanged();
    }

    @Override
    default boolean stillValid(Player player) {
        return getRedirect().stillValid(player);
    }

    @Override
    default void clearContent() {
        getRedirect().clearContent();
    }
}
