package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.configuration.ConfigurationData;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow protected abstract <T> boolean applyImplicitComponentIfPresent(DataComponentGetter from, DataComponentType<T> type);

    @Inject(method = "applyImplicitComponent", at = @At("HEAD"), cancellable = true)
    private void setConfigurationData(DataComponentType<?> type, Object value, CallbackInfoReturnable<Boolean> cir) {
        if (type == FactoryDataComponents.CONFIGURATION_DATA && this instanceof ConfigurableEntity<?> configurableEntity) {
            ConfigurableEntity.applyConfiguration(configurableEntity, (ConfigurationData) value);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "applyImplicitComponents(Lnet/minecraft/core/component/DataComponentGetter;)V", at = @At("TAIL"))
    private void copyConfigurationComponent(DataComponentGetter from, CallbackInfo ci) {
        this.applyImplicitComponentIfPresent(from, FactoryDataComponents.CONFIGURATION_DATA);
    }
}
