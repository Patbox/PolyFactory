package eu.pb4.polyfactory.util.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public interface CustomInsertInventory extends Inventory {
    int insertStack(ItemStack stack, Direction direction);
}
