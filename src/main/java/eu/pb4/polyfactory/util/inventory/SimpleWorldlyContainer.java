package eu.pb4.polyfactory.util.inventory;

import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;

public abstract class SimpleWorldlyContainer extends SimpleContainer implements WorldlyContainer {
    private int[] slots;

    public SimpleWorldlyContainer(int size) {
        super(size);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (this.slots == null) {
            this.slots = IntStream.range(0, this.getContainerSize()).toArray();
        }

        return this.slots;
    }
}
