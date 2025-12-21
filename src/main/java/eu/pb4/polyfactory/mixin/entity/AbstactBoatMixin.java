package eu.pb4.polyfactory.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBoat.class)
public abstract class AbstactBoatMixin implements EntityCatchingVehicle {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;getMaxPassengers()I"))
    private int preventCapture(int original) {
        return this.polyfactory$canCatchEntities() ? original : -1;
    }
}
