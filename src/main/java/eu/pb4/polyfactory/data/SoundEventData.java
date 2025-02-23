package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;

public record SoundEventData(RegistryEntry<SoundEvent> soundEvent, float volume, float pitch, long seed) implements DataContainer {

    public static MapCodec<SoundEventData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Registries.SOUND_EVENT.getEntryCodec().fieldOf("event").forGetter(SoundEventData::soundEvent),
                    Codec.FLOAT.optionalFieldOf("volume", 1f).forGetter(SoundEventData::volume),
                    Codec.FLOAT.optionalFieldOf("pitch", 1f).forGetter(SoundEventData::pitch),
                    Codec.LONG.optionalFieldOf("seed", 0L).forGetter(SoundEventData::seed)
            ).apply(instance, SoundEventData::new)
    );

    @Override
    public DataType<SoundEventData> type() {
        return DataType.SOUND_EVENT;
    }

    @Override
    public String asString() {
        return soundEvent.value().getId().toString();
    }

    @Override
    public long asLong() {
        return (long) volume;
    }

    @Override
    public double asDouble() {
        return this.volume;
    }

    @Override
    public DataContainer extract(String field) {
        return switch (field) {
            case "sound" -> new StringData(asString());
            case "pitch" -> new DoubleData(this.pitch);
            case "volume" -> new DoubleData(this.volume);
            default -> DataContainer.super.extract(field);
        };
    }


    @Override
    public int compareTo(@NotNull DataContainer o) {
        return Double.compare(this.volume, o.asDouble());
    }
}