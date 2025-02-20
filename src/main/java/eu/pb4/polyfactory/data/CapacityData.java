package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public record CapacityData(long stored, long capacity) implements DataContainer {
    public static MapCodec<CapacityData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.optionalFieldOf("stored", 0L).forGetter(CapacityData::stored),
            Codec.LONG.optionalFieldOf("capacity", 0L).forGetter(CapacityData::capacity)
    ).apply(instance, CapacityData::new));

    public static final CapacityData ZERO = new CapacityData(0, 0);
    @Override
    public DataType<CapacityData> type() {
        return DataType.CAPACITY;
    }

    @Override
    public String asString() {
        return "" + stored;
    }

    @Override
    public long asLong() {
        return this.stored;
    }

    @Override
    public double asDouble() {
        return this.stored;
    }

    @Override
    public int asRedstoneOutput() {
        return (int) MathHelper.clamp((15 * this.stored / this.capacity), 0, 15);
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
            case "stored" -> new LongData(this.stored);
            case "capacity" -> new LongData(this.capacity);
            case "percent" -> new DoubleData((double) this.stored / this.capacity);
            default -> DataContainer.empty();
        };
    }
}