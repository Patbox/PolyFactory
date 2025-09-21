package eu.pb4.polyfactory.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VehicleEntity.class)
public class VehicleEntityMixin {
    @Inject(method = "killAndDropItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;set(Lnet/minecraft/component/ComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private void addExtraComponents(ServerWorld world, Item item, CallbackInfo ci, @Local ItemStack stack) {
        if (this instanceof ConfigurableEntity<?> configurableEntity) {
            var config = ConfigurableEntity.extractConfiguration(configurableEntity, false);
            if (!config.isEmpty()) {
                stack.set(FactoryDataComponents.CONFIGURATION_DATA, config);
            }
        }
    }
}
