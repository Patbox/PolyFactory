package eu.pb4.polyfactory.data;

import net.minecraft.nbt.NbtCompound;

public record DoubleData(double value) implements DataContainer {
    @Override
    public DataType type() {
        return DataType.DOUBLE;
    }

    @Override
    public String asString() {
        return "" + value;
    }

    @Override
    public long asLong() {
        return (long) value;
    }

    @Override
    public double asDouble() {
        return value;
    }

    @Override
    public char padding() {
        return '0';
    }

    @Override
    public boolean forceRight() {
        return false;
    }

    @Override
    public void writeNbt(NbtCompound compound) {
        compound.putDouble("value", this.value);
    }

    public static DataContainer fromNbt(NbtCompound compound) {
        return new DoubleData(compound.getDouble("value"));
    }
}