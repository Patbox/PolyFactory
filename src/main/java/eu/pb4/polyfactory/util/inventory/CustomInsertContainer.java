package eu.pb4.polyfactory.util.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface CustomInsertContainer extends Container {
    int insertStack(ItemStack stack, @Nullable Direction direction);
    int insertStackSlots(ItemStack stack, @Nullable Direction direction, IntList slots);
}
