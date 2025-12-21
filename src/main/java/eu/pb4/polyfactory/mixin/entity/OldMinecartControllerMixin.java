package eu.pb4.polyfactory.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartControllerMixin extends MinecartBehavior {
    protected OldMinecartControllerMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @ModifyExpressionValue(method = "pushAndPickupEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;isRideable()Z"))
    private boolean preventCapture(boolean original) {
        return ((EntityCatchingVehicle)this.minecart).polyfactory$canCatchEntities() && original;
    }
}
