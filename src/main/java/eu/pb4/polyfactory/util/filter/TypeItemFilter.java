package eu.pb4.polyfactory.util.filter;

import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record TypeItemFilter(Item item) implements ItemFilter {
    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && stack.is(item);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        TypeItemFilter that = (TypeItemFilter) object;
        return item == that.item;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(item);
    }
}
