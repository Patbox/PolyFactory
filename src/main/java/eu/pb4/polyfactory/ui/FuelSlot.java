package eu.pb4.polyfactory.ui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FuelSlot extends Slot {

    public FuelSlot(Container inventory, int index) {
        super(inventory, index, index, 0);
    }

    public boolean mayPlace(ItemStack stack) {
        return stack.has(DataComponents.COOKING_FUEL) || isBucket(stack);
    }

    public int getMaxStackSize(ItemStack stack) {
        return isBucket(stack) ? 1 : super.getMaxStackSize(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }
}
