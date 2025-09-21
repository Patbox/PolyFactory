package eu.pb4.polyfactory.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.DefaultMinecartController;
import net.minecraft.entity.vehicle.MinecartController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DefaultMinecartController.class)
public abstract class DefaultMinecartControllerMixin extends MinecartController {
    protected DefaultMinecartControllerMixin(AbstractMinecartEntity minecart) {
        super(minecart);
    }

    @ModifyExpressionValue(method = "handleCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;isRideable()Z"))
    private boolean preventCapture(boolean original) {
        return ((EntityCatchingVehicle)this.minecart).polyfactory$canCatchEntities() && original;
    }
}
