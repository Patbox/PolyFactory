package eu.pb4.polyfactory.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VehicleEntity.class)
public class VehicleEntityMixin {
    @Inject(method = "destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/Item;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private void addExtraComponents(ServerLevel world, Item item, CallbackInfo ci, @Local ItemStack stack) {
        if (this instanceof ConfigurableEntity<?> configurableEntity) {
            var config = ConfigurableEntity.extractConfiguration(configurableEntity, false);
            if (!config.isEmpty()) {
                stack.set(FactoryDataComponents.CONFIGURATION_DATA, config);
            }
        }
    }
}
