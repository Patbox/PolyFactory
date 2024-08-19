package eu.pb4.polyfactory.recipe.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import net.minecraft.text.MutableText;
import net.minecraft.util.Unit;

public record FluidInputStack(FluidInstance<?> instance, long required, long used) {
    public static final Codec<FluidInputStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidInstance.MAP_CODEC.forGetter(FluidInputStack::instance),
            Codec.LONG.fieldOf("required").forGetter(FluidInputStack::required),
            Codec.LONG.fieldOf("used").forGetter(FluidInputStack::used)
    ).apply(instance, FluidInputStack::new));

    public static FluidInputStack from(FluidStack<?> stack) {
        return new FluidInputStack(stack.instance(), stack.amount(), stack.amount());
    }

    public FluidInputStack withUsed(long used) {
        return new FluidInputStack(this.instance, this.required, used);
    }

    public FluidInputStack withRequired(long required) {
        return new FluidInputStack(this.instance, required, this.used);
    }

    public MutableText toTextRequired() {
        return this.instance.toLabeledAmount(required);
    }
}
