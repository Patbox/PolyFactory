package eu.pb4.polyfactory.util.inventory;

import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.util.math.Direction;

import java.util.stream.IntStream;

public abstract class SimpleSidedInventory extends SimpleInventory implements SidedInventory {
    private int[] slots;

    public SimpleSidedInventory(int size) {
        super(size);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (this.slots == null) {
            this.slots = IntStream.range(0, this.size()).toArray();
        }

        return this.slots;
    }
}
