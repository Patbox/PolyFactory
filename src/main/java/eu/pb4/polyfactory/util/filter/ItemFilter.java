package eu.pb4.polyfactory.util.filter;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public interface ItemFilter extends Predicate<ItemStack> {
    @Override
    boolean test(ItemStack stack);

    MapCodec<? extends ItemFilter> codec();
}
