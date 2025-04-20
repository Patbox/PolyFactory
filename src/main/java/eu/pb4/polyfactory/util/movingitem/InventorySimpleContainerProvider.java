package eu.pb4.polyfactory.util.movingitem;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;

public interface InventorySimpleContainerProvider extends InventoryContainerHolderProvider{
    SimpleContainer[] getContainers();

    @Override
    default ContainerHolder getContainerHolder(int slot) {
        return this.getContainers()[slot];
    }

    default void readInventoryNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        SimpleContainer.readArray(this.getContainers(), nbt.getListOrEmpty("Items"), lookup);
    }

    default void writeInventoryNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.put("Items", SimpleContainer.writeArray(this.getContainers(), lookup));
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
