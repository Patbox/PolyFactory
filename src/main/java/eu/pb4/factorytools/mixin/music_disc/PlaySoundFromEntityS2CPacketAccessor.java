package eu.pb4.factorytools.mixin.music_disc;

import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlaySoundFromEntityS2CPacket.class)
public interface PlaySoundFromEntityS2CPacketAccessor {
    @Accessor
    RegistryEntry<SoundEvent> getSound();

    @Mutable
    @Accessor
    void setSound(RegistryEntry<SoundEvent> sound);

    @Accessor
    SoundCategory getCategory();

    @Mutable
    @Accessor
    void setCategory(SoundCategory category);

    @Accessor
    int getEntityId();

    @Mutable
    @Accessor
    void setEntityId(int entityId);

    @Accessor
    float getVolume();

    @Mutable
    @Accessor
    void setVolume(float volume);

    @Accessor
    float getPitch();

    @Mutable
    @Accessor
    void setPitch(float pitch);

    @Accessor
    long getSeed();

    @Mutable
    @Accessor
    void setSeed(long seed);
}
