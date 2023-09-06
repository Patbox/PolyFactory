package eu.pb4.polyfactory.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;

import java.util.List;

public record OutputStack(ItemStack stack, float chance, int roll) {
    public static final Codec<OutputStack> CODEC_SELF = RecordCodecBuilder.create(x -> x.group(
                    ItemStack.CODEC.fieldOf("item").forGetter(OutputStack::stack),
                    Codec.FLOAT.optionalFieldOf("chance", 1f).forGetter(OutputStack::chance),
                    Codec.INT.optionalFieldOf("roll", 1).forGetter(OutputStack::roll)
            ).apply(x, OutputStack::new)
    );

    public static final Codec<OutputStack> CODEC = Codec.either(CODEC_SELF, ItemStack.CODEC).xmap(x -> x.map(y -> y, y -> new OutputStack(y, 1f, 1)), x -> Either.left(x));
    public static final Codec<List<OutputStack>> LIST_CODEC = Codec.either(CODEC, Codec.list(CODEC))
            .xmap(x -> x.map(y -> List.of(y), y -> y), x -> x.size() == 1 ? Either.left(x.get(0)) : Either.right(x));

    public static OutputStack of(ItemConvertible stick) {
        return new OutputStack(stick.asItem().getDefaultStack(), 1, 1);
    }
    public static OutputStack of(ItemConvertible stick, float chance, int roll) {
        return new OutputStack(stick.asItem().getDefaultStack(), chance, roll);
    }

    public static OutputStack of(ItemConvertible stick, float chance) {
        return new OutputStack(stick.asItem().getDefaultStack(), chance, 1);
    }
}
