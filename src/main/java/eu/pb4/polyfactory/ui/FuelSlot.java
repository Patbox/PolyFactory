package eu.pb4.polyfactory.ui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.FuelValues;

public class FuelSlot extends Slot {
    private final FuelValues fuelRegistry;

    public FuelSlot(Container inventory, int index, FuelValues fuelRegistry) {
        super(inventory, index, index, 0);
        this.fuelRegistry = fuelRegistry;
    }

    public boolean mayPlace(ItemStack stack) {
        return fuelRegistry.isFuel(stack) || isBucket(stack);
    }

    public int getMaxStackSize(ItemStack stack) {
        return isBucket(stack) ? 1 : super.getMaxStackSize(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }
}
