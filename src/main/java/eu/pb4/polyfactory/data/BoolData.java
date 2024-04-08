package eu.pb4.polyfactory.data;

import net.minecraft.nbt.NbtCompound;

public record BoolData(boolean value) implements DataContainer {
    public static BoolData TRUE = new BoolData(true);
    public static BoolData FALSE = new BoolData(false);

    @Override
    public DataType type() {
        return DataType.BOOL;
    }

    @Override
    public String asString() {
        return value ? "true" : "false";
    }

    @Override
    public long asLong() {
        return value ? 0 : 1;
    }

    @Override
    public double asDouble() {
        return value ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isTrue() {
        return this.value;
    }

    @Override
    public void writeNbt(NbtCompound compound) {
        compound.putBoolean("value", this.value);
    }

    public static DataContainer fromNbt(NbtCompound compound) {
        return compound.getBoolean("value") ? TRUE : FALSE;
    }

}
