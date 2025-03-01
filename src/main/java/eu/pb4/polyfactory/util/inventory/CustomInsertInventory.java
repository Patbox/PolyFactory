package eu.pb4.polyfactory.util.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public interface CustomInsertInventory extends Inventory {
    int insertStack(ItemStack stack, Direction direction);
    int insertStackSlots(ItemStack stack, Direction direction, IntList slots);
}
