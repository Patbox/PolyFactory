package eu.pb4.factorytools.mixin.player;

import eu.pb4.factorytools.impl.ServerPlayNetExtF;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements ServerPlayNetExtF {
    @Unique
    private final VirtualDestroyStage virtualDestroyStage = new VirtualDestroyStage();

    @Override
    public VirtualDestroyStage factorytools$getVirtualDestroyStage() {
        return this.virtualDestroyStage;
    }
}
