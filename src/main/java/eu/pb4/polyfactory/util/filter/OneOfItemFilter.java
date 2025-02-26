package eu.pb4.polyfactory.util.filter;

import net.minecraft.item.ItemStack;

import java.util.Collection;

public record OneOfItemFilter(Collection<ItemFilter> filters, boolean value) implements ItemFilter {
    @Override
    public boolean test(ItemStack stack) {
        for (var filter : filters) {
            if (filter.test(stack)) {
                return this.value;
            }
        }
        return !this.value;
    }
}
