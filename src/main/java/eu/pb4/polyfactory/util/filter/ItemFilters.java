package eu.pb4.polyfactory.util.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Function;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public final class ItemFilters {
    public static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemFilter>> TYPES = new ExtraCodecs.LateBoundIdMapper<>();
    public static final Codec<ItemFilter> CODEC = Codec.lazyInitialized(() -> ItemFilters.TYPES.codec(Identifier.CODEC).dispatch(ItemFilter::codec, Function.identity()));

    public static void bootstrap()  {
        TYPES.put(id("constant"), ConstantItemFilter.CODEC);
        TYPES.put(id("exact_item_stack"), ExactItemFilter.CODEC);
        TYPES.put(id("one_of"), OneOfItemFilter.CODEC);
        TYPES.put(id("item_type"), TypeItemFilter.CODEC);
    }
}