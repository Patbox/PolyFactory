package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import net.minecraft.text.MutableText;

public record FluidStack(FluidType type, long amount) {
    public static final Codec<FluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidType.CODEC.fieldOf("type").forGetter(FluidStack::type),
            Codec.LONG.fieldOf("amount").forGetter(FluidStack::amount)
    ).apply(instance, FluidStack::new));

    public MutableText toTextRequired() {
        return this.type.toLabeledAmount(amount);
    }
}
