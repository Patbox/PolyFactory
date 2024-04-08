package eu.pb4.polyfactory.data;

import net.minecraft.nbt.NbtCompound;

public record StringData(String value) implements DataContainer {
    public static final DataContainer EMPTY = new StringData("");

    public static StringData ofLimited(String s) {
        return new StringData(s.substring(0, Math.min(s.length(), 512)));
    }

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
    public boolean isEmpty() {
        return this.value.isEmpty();
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