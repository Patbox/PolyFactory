package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.polyfactory.block.fluids.DrainBlockEntity;
import eu.pb4.polyfactory.item.FactoryItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin extends Entity {
    @Shadow private Player followingPlayer;
    @Unique
    @Nullable
    private BlockPos drainTarget = null;

    public ExperienceOrbMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;", ordinal = 1))
    private void applyVelocity(CallbackInfo ci) {
        if (this.drainTarget == null) {
            return;
        }

        Vec3 vec3d = new Vec3(this.drainTarget.getX() + 0.5 - this.getX(), this.drainTarget.getY() + 1 - this.getY(), this.drainTarget.getZ() + 0.5 - this.getZ());
        double d = vec3d.lengthSqr();
        if (d < 64.0) {
            double e = 1.0 - Math.sqrt(d) / 8.0;
            this.setDeltaMovement(this.getDeltaMovement().add(vec3d.normalize().scale(e * e * 0.1)));
        }
    }

    @Inject(method = "scanForMerges", at = @At("HEAD"))
    private void findDrains(CallbackInfo ci) {
        if (this.drainTarget != null && this.level().getBlockEntity(this.drainTarget) instanceof DrainBlockEntity be
                && be.catalyst().is(FactoryItemTags.XP_CONVERSION_CATALYST)) {
            return;
        }

        var mut = new BlockPos.MutableBlockPos();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 0; y++) {
                    if (this.level().getBlockEntity(mut.set(this.blockPosition()).move(x, y, z)) instanceof DrainBlockEntity be
                            && be.catalyst().is(FactoryItemTags.XP_CONVERSION_CATALYST)) {
                        this.drainTarget = mut;
                        this.followingPlayer = null;
                        return;
                    }
                }
            }
        }
        this.drainTarget = null;
    }

    @Inject(method = "followNearbyPlayer", at = @At("HEAD"), cancellable = true)
    private void skipPlayerIfTargetIsSet(CallbackInfo ci) {
        if (this.drainTarget != null) {
            ci.cancel();
        }
    }
}
