package eu.pb4.polyfactory.mixin.player;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import eu.pb4.polyfactory.block.collection.BlockCollection;
import eu.pb4.polyfactory.item.util.SwitchActionItem;
import eu.pb4.polyfactory.item.configuration.WrenchHandler;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements ServerPlayNetExt {
    @Shadow private int aboveGroundTickCount;

    @Shadow private int aboveGroundVehicleTickCount;

    @Shadow public abstract ServerPlayer getPlayer();

    @Shadow public ServerPlayer player;
    @Unique
    private final WrenchHandler wrenchHandler = new WrenchHandler((ServerGamePacketListenerImpl) (Object) this);

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.wrenchHandler.tickDisplay(this.getPlayer());
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;getAction()Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;"), cancellable = true)
    private void onAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            var stack = player.getMainHandItem();
            if (stack.getItem() instanceof SwitchActionItem actionItem && actionItem.onSwitchAction(player, stack, InteractionHand.MAIN_HAND)) {
                ci.cancel();
                return;
            }

            stack = player.getOffhandItem();
            if (stack.getItem() instanceof SwitchActionItem actionItem && actionItem.onSwitchAction(player, stack, InteractionHand.OFF_HAND)) {
                ci.cancel();
            }
        }
    }

    @Override
    public void polyFactory$resetFloating() {
        this.aboveGroundTickCount = 0;
        this.aboveGroundVehicleTickCount = 0;
    }

    @WrapMethod(method = "handleMovePlayer")
    private void wrap(ServerboundMovePlayerPacket packet, Operation<Void> original) {
        var apply = this.player.level().getServer().isSameThread();
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
