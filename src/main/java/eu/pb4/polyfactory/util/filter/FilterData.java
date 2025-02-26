package eu.pb4.polyfactory.util.filter;

import eu.pb4.polyfactory.item.tool.AbstractFilterItem;
import net.minecraft.item.ItemStack;

import java.util.List;

public record FilterData(ItemFilter filter, List<ItemStack> icon, boolean prevent) {

    public static final FilterData EMPTY_TRUE = new FilterData(ConstantItemFilter.TRUE, List.of(), false);
    public static final FilterData EMPTY_FALSE = new FilterData(ConstantItemFilter.FALSE, List.of(), true);

    public boolean test(ItemStack stack) {
        return this.filter.test(stack);
    }
    public static FilterData of(ItemStack stack, boolean defaultValue) {
        if (stack.isEmpty()) {
            return defaultValue ? EMPTY_TRUE : EMPTY_FALSE;
        }

        if (stack.getItem() instanceof AbstractFilterItem item && item.isFilterSet(stack)) {
            return item.createFilterData(stack);
        } else {
            stack = stack.copy();
            return new FilterData(new ExactItemFilter(stack), List.of(stack), false);
        }
    }

    public boolean isEmpty() {
        return this.filter instanceof ConstantItemFilter;
    }
}
