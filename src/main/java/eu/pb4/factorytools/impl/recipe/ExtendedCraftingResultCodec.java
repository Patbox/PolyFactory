package eu.pb4.factorytools.impl.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public record ExtendedCraftingResultCodec(Codec<ItemStack> vanillaCodec) implements Codec<ItemStack> {
    @Override
    public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
        var out = this.vanillaCodec.decode(ops, input);

        if (out.result().isPresent()) {
            var maybeNbt = ops.get(input, "factorytools:nbt");
            if (maybeNbt.result().isPresent()) {
                var nbt = NbtCompound.CODEC.decode(ops, maybeNbt.result().get());

                if (nbt.result().isPresent()) {
                    out.result().get().getFirst().setNbt(nbt.result().get().getFirst());
                }
            }
        }
        return out;
    }

    @Override
    public <T> DataResult<T> encode(ItemStack input, DynamicOps<T> ops, T prefix) {
        return this.vanillaCodec.encode(input, ops, prefix);
    }
}
