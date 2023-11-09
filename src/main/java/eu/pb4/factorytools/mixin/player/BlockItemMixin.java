package eu.pb4.factorytools.mixin.player;

import eu.pb4.factorytools.api.block.AbovePlacingLimiter;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void canPlace(ItemPlacementContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        var stateBelow = context.getWorld().getBlockState(context.getBlockPos().down());

        if (stateBelow.getBlock() instanceof AbovePlacingLimiter limiter && !limiter.canPlaceAbove(stateBelow, context, state)) {
            cir.setReturnValue(false);
        }
    }
}
