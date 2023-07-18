package eu.pb4.polyfactory.ui;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

public class FuelSlot extends Slot {
    public FuelSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    public boolean canInsert(ItemStack stack) {
        return FuelRegistry.INSTANCE.get(stack.getItem()) != null || isBucket(stack);
    }

    public int getMaxItemCount(ItemStack stack) {
        return isBucket(stack) ? 1 : super.getMaxItemCount(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.isOf(Items.BUCKET);
    }
}
