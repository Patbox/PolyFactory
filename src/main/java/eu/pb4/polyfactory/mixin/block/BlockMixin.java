package eu.pb4.polyfactory.mixin.block;

import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "isExceptionForConnection", at = @At("HEAD"), cancellable = true)
    private static void dontConnect(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(FactoryBlockTags.UNCONNECTABLE)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyVariable(method = {
            "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            "popResourceFromFace",
            "popExperience"
    }, at = @At("HEAD"), argsOnly = true)
    private static BlockPos handleBlockPosOverride(BlockPos pos) {
        return FactoryUtil.BLOCK_DROPS_POS_OVERRIDE.orElse(pos);
    }
}
