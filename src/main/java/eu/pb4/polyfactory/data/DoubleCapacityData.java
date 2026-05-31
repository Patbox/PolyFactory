package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

public record DoubleCapacityData(double stored, double capacity) implements DataContainer {
    public static final DoubleCapacityData ZERO = new DoubleCapacityData(0, 0);
    public static MapCodec<DoubleCapacityData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("stored", 0d).forGetter(DoubleCapacityData::stored),
            Codec.DOUBLE.optionalFieldOf("capacity", 0d).forGetter(DoubleCapacityData::capacity)
    ).apply(instance, DoubleCapacityData::new));

    @Override
    public DataType<DoubleCapacityData> type() {
        return DataType.DOUBLE_CAPACITY;
    }

    @Override
    public String asString() {
        return "" + stored;
    }

    @Override
    public long asLong() {
        return (long) this.stored;
    }

    @Override
    public double asDouble() {
        return this.stored;
    }

    @Override
    public float asProgress() {
        return (float) (this.stored / Math.max(this.capacity, Double.MIN_NORMAL));
    }

    @Override
    public int asRedstoneOutput() {
        return (int) Mth.clamp((15 * this.stored / this.capacity), 0, 15);
    }

    @Override
    public char padding() {
        return '0';
    }

    @Override
    public boolean forceRight() {
        return true;
    }

    @Override
    public DataContainer extract(String field) {
        return switch (field) {
            case "stored", "value" -> new DoubleData(this.stored);
            case "capacity", "max" -> new DoubleData(this.capacity);
            case "percent" -> new DoubleData(this.stored / this.capacity);
            default -> DataContainer.super.extract(field);
        };
    }
}