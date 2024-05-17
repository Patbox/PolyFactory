package eu.pb4.polyfactory.util.filter;

import net.minecraft.item.ItemStack;

import java.util.Objects;

public record SimpleItemFilter(ItemStack item) implements ItemFilter {
    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(item, stack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleItemFilter that = (SimpleItemFilter) o;
        return that.item.isEmpty() == this.item.isEmpty() && ItemStack.areItemsAndComponentsEqual(that.item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item);
    }
}
