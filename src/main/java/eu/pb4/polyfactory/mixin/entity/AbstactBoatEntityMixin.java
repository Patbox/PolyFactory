package eu.pb4.polyfactory.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBoatEntity.class)
public abstract class AbstactBoatEntityMixin implements EntityCatchingVehicle {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;getMaxPassengers()I"))
    private int preventCapture(int original) {
        return this.polyfactory$canCatchEntities() ? original : -1;
    }
}
