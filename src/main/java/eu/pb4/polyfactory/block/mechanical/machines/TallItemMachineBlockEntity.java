package eu.pb4.polyfactory.block.mechanical.machines;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.util.movingitem.InventorySimpleContainerProvider;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class TallItemMachineBlockEntity extends LockableBlockEntity implements MachineInfoProvider, InventorySimpleContainerProvider, SidedInventory {
    protected Text state;

    public TallItemMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public abstract BlockModel getModel();

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

    @Override
    public @Nullable Text getCurrentState() {
        return this.state;
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        this.openGui((ServerPlayerEntity) player);
        return ActionResult.SUCCESS;
    }
}
