package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.polyfactory.block.fluids.DrainBlockEntity;
import eu.pb4.polyfactory.item.FactoryItemTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity {
    @Shadow private PlayerEntity target;
    @Unique
    @Nullable
    private BlockPos drainTarget = null;

    public ExperienceOrbEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;", ordinal = 1))
    private void applyVelocity(CallbackInfo ci) {
        if (this.drainTarget == null) {
            return;
        }

        Vec3d vec3d = new Vec3d(this.drainTarget.getX() + 0.5 - this.getX(), this.drainTarget.getY() + 1 - this.getY(), this.drainTarget.getZ() + 0.5 - this.getZ());
        double d = vec3d.lengthSquared();
        if (d < 64.0) {
            double e = 1.0 - Math.sqrt(d) / 8.0;
            this.setVelocity(this.getVelocity().add(vec3d.normalize().multiply(e * e * 0.1)));
        }
    }

    @Inject(method = "expensiveUpdate", at = @At("HEAD"))
    private void findDrains(CallbackInfo ci) {
        if (this.drainTarget != null && this.getEntityWorld().getBlockEntity(this.drainTarget) instanceof DrainBlockEntity be
                && be.catalyst().isIn(FactoryItemTags.XP_CONVERSION_CATALYST)) {
            return;
        }

        var mut = new BlockPos.Mutable();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 0; y++) {
                    if (this.getEntityWorld().getBlockEntity(mut.set(this.getBlockPos()).move(x, y, z)) instanceof DrainBlockEntity be
                            && be.catalyst().isIn(FactoryItemTags.XP_CONVERSION_CATALYST)) {
                        this.drainTarget = mut;
                        this.target = null;
                        return;
                    }
                }
            }
        }
        this.drainTarget = null;
    }

    @Inject(method = "moveTowardsPlayer", at = @At("HEAD"), cancellable = true)
    private void skipPlayerIfTargetIsSet(CallbackInfo ci) {
        if (this.drainTarget != null) {
            ci.cancel();
        }
    }
}
