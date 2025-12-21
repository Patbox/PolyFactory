package eu.pb4.polyfactory.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NewMinecartBehavior.class)
public abstract class NewMinecartControllerMixin extends MinecartBehavior {
    protected NewMinecartControllerMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @ModifyExpressionValue(method = {"pushEntities", "pickupEntities"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;isRideable()Z"))
    private boolean preventCapture(boolean original) {
        return ((EntityCatchingVehicle)this.minecart).polyfactory$canCatchEntities() && original;
    }
}
