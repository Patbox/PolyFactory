package eu.pb4.polyfactory.util.filter;

import java.util.Collection;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record OneOfItemFilter(List<ItemFilter> filters, boolean value) implements ItemFilter {
    public static final MapCodec<OneOfItemFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemFilters.CODEC.listOf().fieldOf("filters").forGetter(OneOfItemFilter::filters),
            Codec.BOOL.optionalFieldOf("value", true).forGetter(OneOfItemFilter::value)
    ).apply(instance, OneOfItemFilter::new));

    @Override
    public boolean test(ItemStack stack) {
        for (var filter : filters) {
            if (filter.test(stack)) {
                return this.value;
            }
        }
        return !this.value;
    }

    @Override
    public MapCodec<? extends ItemFilter> codec() {
        return CODEC;
    }
}
