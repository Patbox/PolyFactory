package eu.pb4.polyfactory.mixin.entity;

import eu.pb4.polyfactory.entity.EntityCatchingVehicle;
import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.entity.configurable.EntityConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@Mixin({AbstractMinecart.class, AbstractBoat.class})
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

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCatchDate(ValueOutput view, CallbackInfo ci) {
        view.putBoolean("polyfactory:can_catch_entities", this.canCatchEntities);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readCatchData(ValueInput view, CallbackInfo ci) {
        this.canCatchEntities = view.getBooleanOr("polyfactory:can_catch_entities", true);
    }
}
