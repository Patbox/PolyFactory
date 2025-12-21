package eu.pb4.polyfactory.util.filter;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;

public record ExactItemFilter(ItemStack item) implements ItemFilter {
    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(item, stack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExactItemFilter that = (ExactItemFilter) o;
        return ItemStack.isSameItemSameComponents(that.item, this.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item);
    }
}
