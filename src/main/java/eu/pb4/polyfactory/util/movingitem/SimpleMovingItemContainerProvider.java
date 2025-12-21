package eu.pb4.polyfactory.util.movingitem;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface SimpleMovingItemContainerProvider extends MovingItemContainerHolderProvider {
    SimpleMovingItemContainer[] getContainers();

    @Override
    default MovingItemContainerHolder getContainerHolder(int slot) {
        return this.getContainers()[slot];
    }

    default void readInventoryView(ValueInput view) {
        SimpleMovingItemContainer.readArray(this.getContainers(), view.childrenListOrEmpty("Items"));
    }

    default void writeInventoryView(ValueOutput view) {
        SimpleMovingItemContainer.writeArray(this.getContainers(), view.list("Items", ItemStack.OPTIONAL_CODEC));
    }

    @Override
    default int getContainerSize() {
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
    default boolean stillValid(Player player) {
        return true;
    }

    @Override
    default void clearContent() {
        for (var x : this.getContainers()) {
            x.clearContainer();
        }
    }
}
