package eu.pb4.polyfactory.util.movingitem;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

public interface InventorySimpleContainerProvider extends InventoryContainerHolderProvider{
    SimpleContainer[] getContainers();

    @Override
    default ContainerHolder getContainerHolder(int slot) {
        return this.getContainers()[slot];
    }

    default void readInventoryView(ReadView view) {
        SimpleContainer.readArray(this.getContainers(), view.getListReadView("Items"));
    }

    default void writeInventoryView(WriteView view) {
        SimpleContainer.writeArray(this.getContainers(), view.getListAppender("Items", ItemStack.OPTIONAL_CODEC));
    }

    @Override
    default int size() {
        return this.getContainers().length;
    }

    @Override
    default boolean isEmpty() {
        var c = this.getContainers();
        for (int i = 0; i < c.length; i++) {
            if (!c[i].isContainerEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    default void clear() {
        for (var x : this.getContainers()) {
            x.clearContainer();
        }
    }
}
