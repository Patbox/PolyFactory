package eu.pb4.polyfactory.data;

import net.minecraft.nbt.NbtCompound;

public record LongData(long value) implements DataContainer {
    @Override
    public DataType type() {
        return DataType.LONG;
    }

    @Override
    public String asString() {
        return "" + value;
    }

    @Override
    public long asLong() {
        return value;
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
        return true;
    }

    @Override
    public void writeNbt(NbtCompound compound) {
        compound.putLong("value", this.value);
    }

    public static DataContainer fromNbt(NbtCompound compound) {
        return new LongData(compound.getLong("value"));
    }
}