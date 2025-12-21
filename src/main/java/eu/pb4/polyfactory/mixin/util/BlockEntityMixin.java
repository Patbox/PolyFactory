package eu.pb4.polyfactory.mixin.util;

import eu.pb4.polyfactory.block.other.CustomBlockEntityCalls;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {
    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void polyfactory$onMarkedRemoved(CallbackInfo ci) {
        if (this instanceof CustomBlockEntityCalls customBlockEntityCalls) {
            customBlockEntityCalls.polyfactory$markRemoved();
        }
    }

    @Inject(method = "setBlockState", at = @At("TAIL"))
    private void polyfactory$onSetCachedState(BlockState state, CallbackInfo ci) {
        if (this instanceof CustomBlockEntityCalls customBlockEntityCalls) {
            customBlockEntityCalls.polyfactory$setCachedState(state);
        }
    }
}
