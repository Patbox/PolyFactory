package eu.pb4.polyfactory.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.block.SneakBypassingBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @ModifyExpressionValue(
            method = "interactBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;shouldCancelInteraction()Z")
    )
    private boolean polyfactory$dontCancelForSome(boolean original, @Local BlockState state) {
        return !(state.getBlock() instanceof SneakBypassingBlock) && original;
    }
}
