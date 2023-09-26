package eu.pb4.polyfactory.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.block.base.SneakBypassingBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @ModifyExpressionValue(
            method = "interactBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;shouldCancelInteraction()Z")
    )
    private boolean dontCancelForSome(boolean original, @Local BlockState state) {
        if (this.player.getMainHandStack().isOf(FactoryItems.WRENCH)) {
            return true;
        } else {
            return !(state.getBlock() instanceof SneakBypassingBlock) && original;
        }
    }
}
