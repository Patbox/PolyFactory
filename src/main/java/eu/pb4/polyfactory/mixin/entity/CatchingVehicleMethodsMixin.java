package eu.pb4.polyfactory.mixin.entity;

import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.entity.configurable.EntityConfig;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin({AbstractMinecartEntity.class, AbstractBoatEntity.class})
public class CatchingVehicleMethodsMixin implements EntityCatchingVehicle {
    @Unique
    private boolean canCatchEntities = true;

    @Override
    public boolean polyfactory$canCatchEntities() {
        return this.canCatchEntities;
    }

    @Override
    public void polyfactory$setCatchEntities(boolean value) {
        this.canCatchEntities = value;
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void writeCatchDate(WriteView view, CallbackInfo ci) {
        view.putBoolean("polyfactory:can_catch_entities", this.canCatchEntities);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void readCatchData(ReadView view, CallbackInfo ci) {
        this.canCatchEntities = view.getBoolean("polyfactory:can_catch_entities", true);
    }
}
