package eu.pb4.polyfactory.block.mechanical.machines;

import eu.pb4.polyfactory.block.base.LockableBlockEntity;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.util.movingitem.InventorySimpleContainerProvider;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public abstract class TallItemMachineBlockEntity extends LockableBlockEntity implements InventorySimpleContainerProvider, SidedInventory {
    public TallItemMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public abstract BaseModel getModel();

    public abstract double getStress();

    protected abstract void updatePosition(int id);

    protected void addMoving(int i, MovingItem x, boolean newlyAdded) {
        var model = this.getModel();
        if (model != null) {
            if (newlyAdded) {
                updatePosition(i);
                model.addElement(x);
            } else {
                model.addElementWithoutUpdates(x);
                updatePosition(i);
            }
        }
        this.markDirty();
    }

    protected void removeMoving(MovingItem movingItem, boolean fullRemove) {
        var model = this.getModel();

        if (model != null) {
            if (fullRemove) {
                model.removeElement(movingItem);
            } else {
                model.removeElementWithoutUpdates(movingItem);
            }
        }
        this.markDirty();
    }
}
