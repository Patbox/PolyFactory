package eu.pb4.polyfactory.mixin.util;

import eu.pb4.polyfactory.DynamicContent;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin implements Registry<Object> {
    @Inject(method = "freeze", at = @At("HEAD"))
    private void dynamicGeneration(CallbackInfoReturnable<Registry<?>> cir) {
        DynamicContent.registerDynamicEntries(this);
    }
}
