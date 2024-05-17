package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;


public record GameEventData(GameEvent event, Vec3d pos, double distance) implements DataContainer {
    private static final MapCodec<Vec3d> FLAT_VEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.DOUBLE.fieldOf("pos_x").forGetter(Vec3d::getX),
            Codec.DOUBLE.fieldOf("pos_y").forGetter(Vec3d::getY),
            Codec.DOUBLE.fieldOf("pos_z").forGetter(Vec3d::getZ)
    ).apply(ins, Vec3d::new));

    public static MapCodec<GameEventData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Registries.GAME_EVENT.getCodec().fieldOf("event").forGetter(GameEventData::event),
                    FLAT_VEC.forGetter(GameEventData::pos),
                    Codec.DOUBLE.fieldOf("distance").forGetter(GameEventData::distance)
            ).apply(instance, GameEventData::new)
    );

    public static DataContainer fromNbt(NbtCompound compound) {
        return new GameEventData(
                Registries.GAME_EVENT.get(Identifier.tryParse(compound.getString("event"))),
                new Vec3d(compound.getDouble("pos_x"), compound.getDouble("pos_y"), compound.getDouble("pos_z")),
                compound.getDouble("distance")
        );
    }

    @Override
    public DataType<GameEventData> type() {
        return DataType.GAME_EVENT;
    }

    @Override
    public String asString() {
        return Registries.GAME_EVENT.getId(event).toString();
    }

    @Override
    public long asLong() {
        return (long) this.distance;
    }

    @Override
    public double asDouble() {
        return this.distance;
    }
}