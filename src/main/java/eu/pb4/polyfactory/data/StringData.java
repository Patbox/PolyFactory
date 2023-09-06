package eu.pb4.polyfactory.data;

import net.minecraft.nbt.NbtCompound;

public record StringData(String value) implements DataContainer {
    public static final DataContainer EMPTY = new StringData("");

    @Override
    public DataType type() {
        return DataType.STRING;
    }

    @Override
    public String asString() {
        return value;
    }

    @Override
    public long asLong() {
        return value.length();
    }

    @Override
    public double asDouble() {
        return value.length();
    }

    @Override
    public boolean forceRight() {
        return false;
    }

    @Override
    public void writeNbt(NbtCompound compound) {
        compound.putString("value", this.value);
    }

    public static DataContainer fromNbt(NbtCompound compound) {
        return new StringData(compound.getString("value"));
    }
}