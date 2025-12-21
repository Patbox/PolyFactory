package eu.pb4.polyfactory.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.block.other.TagRedirector;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow public abstract Block getBlock();

    @ModifyReturnValue(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("TAIL"))
    private boolean extendedIsIn(boolean original, @Local(argsOnly = true) TagKey<Block> tagKey) {
        return this.getBlock() instanceof TagRedirector tagRedirector ? tagRedirector.customIsIn(tagKey, original) : original;
    }

    @ModifyReturnValue(method = "is(Lnet/minecraft/core/HolderSet;)Z", at = @At("TAIL"))
    private boolean extendedIsIn2(boolean original, @Local(argsOnly = true) HolderSet<Block> entryList) {
        return this.getBlock() instanceof TagRedirector tagRedirector ? tagRedirector.customIsIn(entryList, original) : original;
    }
}
