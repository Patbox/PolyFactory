package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import eu.pb4.polyfactory.util.movingitem.SimpleMovingItemContainerProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface SimpleMovingItemContainerBlockEntity extends SimpleMovingItemContainerProvider, WorldlyContainer {
    @Nullable
    BlockModel getModel();

    void updatePosition(int id);

    default void addMoving(int i, MovingItem movingItem, boolean newlyAdded) {
        var model = this.getModel();
        if (model != null) {
            if (newlyAdded || !movingItem.wasVisible()) {
                updatePosition(i);
                model.addElement(movingItem);
                movingItem.setWasVisible(true);
            } else {
                model.addElementWithoutUpdates(movingItem);
                updatePosition(i);
            }
        }
        this.setChanged();
    }

    default void removeMoving(MovingItem movingItem, boolean fullRemove) {
        var model = this.getModel();

        if (model != null) {
            if (fullRemove) {
                model.removeElement(movingItem);
                movingItem.setWasVisible(false);
            } else {
                model.removeElementWithoutUpdates(movingItem);
            }
        }
        this.setChanged();
    }
}
