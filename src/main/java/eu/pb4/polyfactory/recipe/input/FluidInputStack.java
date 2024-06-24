package eu.pb4.polyfactory.recipe.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidType;
import net.minecraft.text.MutableText;

public record FluidInputStack(FluidType type, long required, long used) {
    public static final Codec<FluidInputStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidType.CODEC.fieldOf("type").forGetter(FluidInputStack::type),
            Codec.LONG.fieldOf("required").forGetter(FluidInputStack::required),
            Codec.LONG.fieldOf("used").forGetter(FluidInputStack::used)
    ).apply(instance, FluidInputStack::new));

    public MutableText toTextRequired() {
        return this.type.toLabeledAmount(required);
    }
}
