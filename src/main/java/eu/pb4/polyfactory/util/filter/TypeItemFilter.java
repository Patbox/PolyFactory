package eu.pb4.polyfactory.util.filter;

import java.util.Objects;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record TypeItemFilter(Item item) implements ItemFilter {
    public static final MapCodec<TypeItemFilter> CODEC = BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").xmap(TypeItemFilter::new, TypeItemFilter::item);

    @Override
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && stack.is(item);
    }

    @Override
    public MapCodec<? extends ItemFilter> codec() {
        return CODEC;
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
