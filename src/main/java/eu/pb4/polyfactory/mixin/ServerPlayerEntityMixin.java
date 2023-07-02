package eu.pb4.polyfactory.mixin;

import eu.pb4.polyfactory.util.FactoryPlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "moveToSpawn", at = @At("HEAD"), cancellable = true)
    private void polyfactory$dontMoveFakePlayers(ServerWorld world, CallbackInfo ci) {
        if (((Object) this) instanceof FactoryPlayer) {
            ci.cancel();
        }
    }
}
