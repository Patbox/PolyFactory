package eu.pb4.polyfactory.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.block.other.TagRedirector;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {
    @Shadow public abstract Block getBlock();

    @ModifyReturnValue(method = "isIn(Lnet/minecraft/registry/tag/TagKey;)Z", at = @At("TAIL"))
    private boolean extendedIsIn(boolean original, @Local(argsOnly = true) TagKey<Block> tagKey) {
        return this.getBlock() instanceof TagRedirector tagRedirector ? tagRedirector.customIsIn(tagKey, original) : original;
    }

    @ModifyReturnValue(method = "isIn(Lnet/minecraft/registry/entry/RegistryEntryList;)Z", at = @At("TAIL"))
    private boolean extendedIsIn2(boolean original, @Local(argsOnly = true) RegistryEntryList<Block> entryList) {
        return this.getBlock() instanceof TagRedirector tagRedirector ? tagRedirector.customIsIn(entryList, original) : original;
    }
}
