package eu.pb4.polyfactory.mixin.player;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import eu.pb4.polyfactory.block.collection.BlockCollection;
import eu.pb4.polyfactory.item.util.SwitchActionItem;
import eu.pb4.polyfactory.item.wrench.WrenchHandler;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements ServerPlayNetExt {
    @Shadow private int floatingTicks;

    @Shadow private int vehicleFloatingTicks;

    @Shadow public abstract ServerPlayerEntity getPlayer();

    @Shadow public ServerPlayerEntity player;
    @Unique
    private final WrenchHandler wrenchHandler = new WrenchHandler((ServerPlayNetworkHandler) (Object) this);

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.wrenchHandler.tick(this.getPlayer());
    }

    @Inject(method = "onPlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;getAction()Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;"), cancellable = true)
    private void onAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (packet.getAction() == PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            var stack = player.getMainHandStack();
            if (stack.getItem() instanceof SwitchActionItem actionItem && actionItem.onSwitchAction(player, stack, Hand.MAIN_HAND)) {
                ci.cancel();
                return;
            }

            stack = player.getOffHandStack();
            if (stack.getItem() instanceof SwitchActionItem actionItem && actionItem.onSwitchAction(player, stack, Hand.OFF_HAND)) {
                ci.cancel();
            }
        }
    }

    @Override
    public void polyFactory$resetFloating() {
        this.floatingTicks = 0;
        this.vehicleFloatingTicks = 0;
    }

    @WrapMethod(method = "onPlayerMove")
    private void wrap(PlayerMoveC2SPacket packet, Operation<Void> original) {
        var apply = this.player.getServerWorld().getServer().isOnThread();
        if (apply) {
            BlockCollection.ignoreCollisions = true;
        }
        original.call(packet);
        if (apply) {
            BlockCollection.ignoreCollisions = false;
        }
    }

    @Override
    public WrenchHandler polyFactory$getWrenchHandler() {
        return this.wrenchHandler;
    }

}
