package eu.pb4.polyfactory.util.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface CustomInsertContainer extends Container {
    int insertStack(ItemStack stack, Direction direction);
    int insertStackSlots(ItemStack stack, Direction direction, IntList slots);
}
