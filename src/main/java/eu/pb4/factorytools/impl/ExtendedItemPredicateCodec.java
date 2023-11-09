package eu.pb4.factorytools.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;

public record ExtendedItemPredicateCodec(Codec<ItemPredicate> vanillaCodec) implements Codec<ItemPredicate> {
    @Override
    public <T> DataResult<Pair<ItemPredicate, T>> decode(DynamicOps<T> ops, T input) {
        var out = this.vanillaCodec.decode(ops, input);

        if (out.result().isPresent()) {
            var maybeNbt = ops.get(input, "factorytools:static_predicate");
            if (maybeNbt.result().isPresent()) {
                var nbt = Identifier.CODEC.decode(ops, maybeNbt.result().get());

                if (nbt.result().isPresent()) {
                    ((ExtraItemPredicate) (Object) out.result().get().getFirst()).factorytools$setStaticPredicate(nbt.result().get().getFirst());
                }
            }
        }
        return out;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public <T> DataResult<T> encode(ItemPredicate input, DynamicOps<T> ops, T prefix) {
        var result = this.vanillaCodec.encode(input, ops, prefix);
        var ext = (ExtraItemPredicate) (Object) input;
        if (result.result().isPresent()) {
            var value = result.result().get();
            if (ext.factorytools$getStaticPredicate() != null) {
                result = ops.mergeToMap(value, ops.createString("factorytools:static_predicate"),
                        ops.createString(ext.factorytools$getStaticPredicate().toString()));
            }
        }
        return result;
    }
}
