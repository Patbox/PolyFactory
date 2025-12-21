package eu.pb4.polyfactory.ui;

import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PredicateLimitedSlot extends Slot {
    private final Predicate<ItemStack> predicate;

    public PredicateLimitedSlot(Container inventory, int index, Predicate<ItemStack> predicate) {
        super(inventory, index, index, 0);
        this.predicate = predicate;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return predicate.test(stack);
    }
}
