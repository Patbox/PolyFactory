package eu.pb4.polyfactory.util.filter;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Objects;

public record TypeItemFilter(Item item) implements ItemFilter {
    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(item);
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
