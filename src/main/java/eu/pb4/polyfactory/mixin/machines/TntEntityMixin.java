package eu.pb4.polyfactory.mixin.machines;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TntEntity.class)
public abstract class TntEntityMixin extends Entity implements LastFanEffectedTickConsumer {
    @Unique
    private int lastFanAge = -9999;

    public TntEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/TntEntity;isOnGround()Z"))
    private boolean canBePlaced(boolean value) {
        return value && this.age - this.lastFanAge > 10;
    }

    /*@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void addExtraNbt(NbtCompound nbt, CallbackInfo ci) {
        view.putInt("polyfactory:last_fan_age", this.lastFanAge);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readExtraNbt(NbtCompound nbt, CallbackInfo ci) {
        this.lastFanAge = view.getInt("polyfactory:last_fan_age");
    }*/

    @Override
    public void polyfactory$setLastFanTick() {
        this.lastFanAge = this.age;
    }

    @Override
    public int polyfactory$getLastOnGround() {
        return this.lastFanAge;
    }

    @Override
    public double polyfactory$getLastY() {
        return this.getY();
    }
}
