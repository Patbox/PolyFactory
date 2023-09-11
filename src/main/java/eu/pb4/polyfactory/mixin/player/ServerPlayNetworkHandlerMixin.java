package eu.pb4.polyfactory.mixin.player;

import eu.pb4.polyfactory.item.wrench.WrenchHandler;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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

    @Unique
    private final VirtualDestroyStage virtualDestroyStage = new VirtualDestroyStage();

    @Unique
    private final WrenchHandler wrenchHandler = new WrenchHandler((ServerPlayNetworkHandler) (Object) this);

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.wrenchHandler.tick(this.getPlayer());
    }

    @Override
    public void polyFactory$resetFloating() {
        this.floatingTicks = 0;
        this.vehicleFloatingTicks = 0;
    }

    @Override
    public VirtualDestroyStage polyFactory$getVirtualDestroyStage() {
        return this.virtualDestroyStage;
    }

    @Override
    public WrenchHandler polyFactory$getWrenchHandler() {
        return this.wrenchHandler;
    }
}
