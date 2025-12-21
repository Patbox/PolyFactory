package eu.pb4.polyfactory.recipe.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;

public record FluidProportion(FluidInstance<?> instance, float proportion) {
    public static final Codec<FluidProportion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidInstance.MAP_CODEC.forGetter(FluidProportion::instance),
            Codec.FLOAT.fieldOf("proportion").forGetter(FluidProportion::proportion)
    ).apply(instance, FluidProportion::new));

    /*public MutableText toTextRequired() {
        return this.instance.toLabeledAmount(required);
    }*/
}
