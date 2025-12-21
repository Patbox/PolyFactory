package eu.pb4.polyfactory.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
    @Accessor
    void setDuration(int duration);

    @Accessor
    void setHiddenEffect(MobEffectInstance hiddenEffect);
}
