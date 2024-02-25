package eu.pb4.polyfactory.mixin.block;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import eu.pb4.polyfactory.util.ColorProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {
    @Shadow @Nullable public abstract BlockEntity getBlockEntity(BlockPos pos);

    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;hasBlockEntity()Z", ordinal = 0))
    private void setDefault(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir, @Share("color") LocalIntRef color, @Local(ordinal = 1) BlockState oldState) {
        if (oldState.hasBlockEntity() && this.getBlockEntity(pos) instanceof ColorProvider provider) {
            color.set(provider.getColor());
        } else {
            color.set(-1);
        }
    }

    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;addBlockEntity(Lnet/minecraft/block/entity/BlockEntity;)V", shift = At.Shift.BEFORE))
    private void setColor(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir, @Local BlockEntity blockEntity, @Share("color") LocalIntRef color) {
        var c = color.get();
        if (blockEntity instanceof ColorProvider provider && c != -1) {
            provider.setColorFromPreviousBlockEntity(c);
        }
    }
}
