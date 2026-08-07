package eu.pb4.polyfactory.util.filter;

import java.util.Objects;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;

public record ExactItemFilter(ItemStack item) implements ItemFilter {
    public static final MapCodec<ExactItemFilter> CODEC = ItemStack.OPTIONAL_CODEC.fieldOf("stack").xmap(ExactItemFilter::new, ExactItemFilter::item);

    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(item, stack);
    }

    @Override
    public MapCodec<? extends ItemFilter> codec() {
        return CODEC;
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
