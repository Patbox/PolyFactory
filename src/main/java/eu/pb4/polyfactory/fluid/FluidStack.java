package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.util.Unit;

public record FluidStack<T>(FluidInstance<T> instance, long amount) {
    public static final Codec<FluidStack<?>> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidInstance.MAP_CODEC.forGetter(FluidStack::instance),
            Codec.LONG.fieldOf("amount").forGetter(FluidStack::amount)
    ).apply(instance, FluidStack::new));
    public FluidStack(FluidType<T> type, T data, long amount) {
        this(type.toInstance(data), amount);
    }

    public FluidType<T> type() {
        return this.instance.type();
    }

    public T data() {
        return this.instance.data();
    }

    public MutableText toTextRequired() {
        return this.instance.toLabeledAmount(amount);
    }

    public boolean isEmpty() {
        return this.amount <= 0;
    }

    public FluidStack<T> withAmount(long amount) {
        return new FluidStack<>(this.instance, amount);
    }
}
