package eu.pb4.polyfactory.util.filter;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Objects;

public record ConstantItemFilter(boolean value) implements ItemFilter {
    public static final ItemFilter TRUE = new ConstantItemFilter(true);
    public static final ItemFilter FALSE = new ConstantItemFilter(false);

    @Override
    public boolean test(ItemStack stack) {
        return this.value;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstantItemFilter that = (ConstantItemFilter) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
