package eu.pb4.polyfactory.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;


public record GameEventData(GameEvent event, Vec3d pos, double distance) implements DataContainer {
    @Override
    public DataType type() {
        return DataType.GAME_EVENT;
    }

    @Override
    public String asString() {
        return event.getId();
    }

    @Override
    public long asLong() {
        return (long) this.distance;
    }

    @Override
    public double asDouble() {
        return this.distance;
    }

    @Override
    public void writeNbt(NbtCompound compound) {
        compound.putString("event", event.getId());
        compound.putDouble("pos_x", this.pos.x);
        compound.putDouble("pos_y", this.pos.y);
        compound.putDouble("pos_z", this.pos.z);
        compound.putDouble("distance", this.distance);
    }

    public static DataContainer fromNbt(NbtCompound compound) {
        return new GameEventData(
                Registries.GAME_EVENT.get(Identifier.tryParse(compound.getString("event"))),
                new Vec3d(compound.getDouble("pos_x"), compound.getDouble("pos_y"), compound.getDouble("pos_z")),
                compound.getDouble("distance")
        );
    }
}