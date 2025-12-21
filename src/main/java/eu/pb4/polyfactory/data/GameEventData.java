package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;


public record GameEventData(GameEvent event, Vec3 pos, double distance) implements DataContainer {
    private static final MapCodec<Vec3> FLAT_VEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Codec.DOUBLE.fieldOf("pos_x").forGetter(Vec3::x),
            Codec.DOUBLE.fieldOf("pos_y").forGetter(Vec3::y),
            Codec.DOUBLE.fieldOf("pos_z").forGetter(Vec3::z)
    ).apply(ins, Vec3::new));

    public static MapCodec<GameEventData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BuiltInRegistries.GAME_EVENT.byNameCodec().fieldOf("event").forGetter(GameEventData::event),
                    FLAT_VEC.forGetter(GameEventData::pos),
                    Codec.DOUBLE.fieldOf("distance").forGetter(GameEventData::distance)
            ).apply(instance, GameEventData::new)
    );

    @Override
    public DataType<GameEventData> type() {
        return DataType.GAME_EVENT;
    }

    @Override
    public String asString() {
        return BuiltInRegistries.GAME_EVENT.getKey(event).toString();
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
    public DataContainer extract(String field) {
        return switch (field) {
            case "event" -> new StringData(asString());
            case "position" -> new StringData(pos.toString());
            case "distance" -> new DoubleData(this.distance);
            default -> DataContainer.super.extract(field);
        };
    }

    @Override
    public int compareTo(@NotNull DataContainer o) {
        return Double.compare(this.distance, o.asDouble());
    }
}