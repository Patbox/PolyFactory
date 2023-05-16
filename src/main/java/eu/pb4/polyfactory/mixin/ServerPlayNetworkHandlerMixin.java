package eu.pb4.polyfactory.mixin;

import eu.pb4.polyfactory.util.ServerPlayNetExt;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements ServerPlayNetExt {
    @Shadow private int floatingTicks;

    @Shadow private int vehicleFloatingTicks;

    @Override
    public void polyFactory$resetFloating() {
        this.floatingTicks = 0;
        this.vehicleFloatingTicks = 0;
    }
}
