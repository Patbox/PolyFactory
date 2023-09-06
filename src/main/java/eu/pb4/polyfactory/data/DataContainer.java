package eu.pb4.polyfactory.data;

import net.minecraft.nbt.NbtCompound;

public interface DataContainer {
    static DataContainer of(long count) {
        return new LongData(count);
    }

    DataType type();

    String asString();
    long asLong();
    double asDouble();
    default char padding() {
        return ' ';
    }

    default boolean forceRight() {
        return false;
    };

    void writeNbt(NbtCompound compound);

    static DataContainer fromNbt(NbtCompound compound) {
        var type = DataType.TYPES.get(compound.getString("type"));
        if (type != null) {
            return type.nbtReader().apply(compound);
        }
        return StringData.EMPTY;
    }

    default NbtCompound createNbt() {
        var nbt = new NbtCompound();
        nbt.putString("type", this.type().id());
        writeNbt(nbt);
        return nbt;
    }
}
