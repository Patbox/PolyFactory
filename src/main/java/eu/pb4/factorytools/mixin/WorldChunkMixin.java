package eu.pb4.factorytools.mixin;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {
    @Inject(method = "updateGameEventListener", at = @At("TAIL"))
    private void callMethod(BlockEntity blockEntity, ServerWorld world, CallbackInfo ci) {
        if (blockEntity instanceof BlockEntityExtraListener listener) {
            listener.onListenerUpdate((WorldChunk) (Object) this);
        }
    }
}
