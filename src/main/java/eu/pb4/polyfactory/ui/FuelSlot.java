package eu.pb4.polyfactory.ui;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

public class FuelSlot extends Slot {
    private final FuelRegistry fuelRegistry;

    public FuelSlot(Inventory inventory, int index, FuelRegistry fuelRegistry) {
        super(inventory, index, index, 0);
        this.fuelRegistry = fuelRegistry;
    }

    public boolean canInsert(ItemStack stack) {
        return fuelRegistry.isFuel(stack) || isBucket(stack);
    }

    public int getMaxItemCount(ItemStack stack) {
        return isBucket(stack) ? 1 : super.getMaxItemCount(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.isOf(Items.BUCKET);
    }
}
