package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.configuration.ConfigurationData;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow protected abstract <T> boolean copyComponentFrom(ComponentsAccess from, ComponentType<T> type);

    @Inject(method = "setApplicableComponent", at = @At("HEAD"), cancellable = true)
    private void setConfigurationData(ComponentType<?> type, Object value, CallbackInfoReturnable<Boolean> cir) {
        if (type == FactoryDataComponents.CONFIGURATION_DATA && this instanceof ConfigurableEntity<?> configurableEntity) {
            ConfigurableEntity.applyConfiguration(configurableEntity, (ConfigurationData) value);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "copyComponentsFrom(Lnet/minecraft/component/ComponentsAccess;)V", at = @At("TAIL"))
    private void copyConfigurationComponent(ComponentsAccess from, CallbackInfo ci) {
        this.copyComponentFrom(from, FactoryDataComponents.CONFIGURATION_DATA);
    }
}
