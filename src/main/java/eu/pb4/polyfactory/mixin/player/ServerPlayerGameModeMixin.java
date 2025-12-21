package eu.pb4.polyfactory.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.pb4.polyfactory.item.FactoryItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @ModifyExpressionValue(
            method = "useItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z")
    )
    private boolean dontCancelForSome(boolean original) {
        if (this.player.getMainHandItem().is(FactoryItems.WRENCH)) {
            return true;
        }
        return original;
    }
}
