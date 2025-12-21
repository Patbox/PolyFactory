package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;

public record SoundEventData(Holder<SoundEvent> soundEvent, float volume, float pitch, long seed) implements DataContainer {

    public static MapCodec<SoundEventData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BuiltInRegistries.SOUND_EVENT.holderByNameCodec().fieldOf("event").forGetter(SoundEventData::soundEvent),
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
        return soundEvent.value().location().toString();
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