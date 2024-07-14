package eu.pb4.polyfactory.recipe.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import net.minecraft.text.MutableText;

public record FluidInputStack(FluidInstance<?> instance, long required, long used) {
    public static final Codec<FluidInputStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidInstance.CODEC.forGetter(FluidInputStack::instance),
            Codec.LONG.fieldOf("required").forGetter(FluidInputStack::required),
            Codec.LONG.fieldOf("used").forGetter(FluidInputStack::used)
    ).apply(instance, FluidInputStack::new));

    public MutableText toTextRequired() {
        return this.instance.toLabeledAmount(required);
    }
}
