package eu.pb4.polyfactory.util.filter;

import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;

import java.util.function.Predicate;

public record FilterData(ItemFilter filter, ItemStack icon) {

    public static final FilterData EMPTY_TRUE = new FilterData(ConstantItemFilter.TRUE, ItemStack.EMPTY);
    public static final FilterData EMPTY_FALSE = new FilterData(ConstantItemFilter.FALSE, ItemStack.EMPTY);

    public boolean test(ItemStack stack) {
        return this.filter.test(stack);
    }
    public static FilterData of(ItemStack stack, boolean defaultValue) {
        if (stack.isEmpty()) {
            return defaultValue ? EMPTY_TRUE : EMPTY_FALSE;
        }

        ItemStack testStack;

        if (stack.isOf(FactoryItems.ITEM_FILTER) && stack.hasNbt() && stack.getNbt().contains("item", NbtElement.COMPOUND_TYPE)) {
            testStack = ItemStack.fromNbt(stack.getNbt().getCompound("item"));
        } else {
            testStack = stack;
        }
        return new FilterData(new SimpleItemFilter(testStack), testStack);
    }

    public boolean isEmpty() {
        return this.filter instanceof ConstantItemFilter;
    }
}
