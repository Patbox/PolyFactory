package eu.pb4.polyfactory.mixin.player;

import eu.pb4.polyfactory.block.mechanical.machines.MinerBlockEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow public abstract LivingEntity getLastAttacker();

    /*@Inject(method = "dropXp", at = @At("HEAD"), cancellable = true)
    private void dontDropXpForMiners(CallbackInfo ci) {
        if (this.getLastAttacker() instanceof MinerBlockEntity.MinerPlayer) {
            ci.cancel();
        }
    }*/
}
