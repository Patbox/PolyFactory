package eu.pb4.polyfactory.mixin.player;

import eu.pb4.polyfactory.util.ServerPlayNetExt;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements ServerPlayNetExt {

    @Shadow private int floatingTicks;

    @Shadow private int vehicleFloatingTicks;

    @Unique
    private final VirtualDestroyStage virtualDestroyStage = new VirtualDestroyStage();

    @Override
    public void polyFactory$resetFloating() {
        this.floatingTicks = 0;
        this.vehicleFloatingTicks = 0;
    }

    @Override
    public VirtualDestroyStage polyFactory$getVirtualDestroyStage() {
        return this.virtualDestroyStage;
    }
}
