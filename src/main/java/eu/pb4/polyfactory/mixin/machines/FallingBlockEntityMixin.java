package eu.pb4.polyfactory.mixin.machines;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity implements LastFanEffectedTickConsumer {
    @Unique
    private int lastFanAge = -9999;

    public FallingBlockEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;onGround()Z"))
    private boolean canBePlaced(boolean value) {
        return value && this.tickCount - this.lastFanAge > 10;
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
        this.lastFanAge = this.tickCount;
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
