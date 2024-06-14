//
// Source code recreated from a .class file by Quiltflower
//

package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.block.electric.WitherSkullGeneratorBlock;
import eu.pb4.polyfactory.block.electric.WitherSkullGeneratorBlockEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;
import net.minecraft.world.explosion.Explosion;

public class ArtificialWitherSkullEntity extends ExplosiveProjectileEntity implements PolymerEntity {
    public ArtificialWitherSkullEntity(EntityType<ArtificialWitherSkullEntity> entityType, World world) {
        super(entityType, world);
    }

    public static ArtificialWitherSkullEntity create(ServerWorld world, BlockPos pos, Direction dir, PlayerEntity owner) {
        var entity = new ArtificialWitherSkullEntity(FactoryEntities.ARTIFICIAL_WITHER_SKULL, world);

        if (owner != null) {
            entity.setOwner(owner);
        }


        entity.setPosition(Vec3d.ofCenter(pos, 4 / 16f).offset(dir, 0.8));
        entity.accelerationPower = 0.2;

        //double g = Math.sqrt(dir.getOffsetZ() + dir.getOffsetX());
        //entity.setPitch(MathHelper.wrapDegrees((float)(-(MathHelper.atan2(dir.getOffsetY(), g) * 180.0F / (float)Math.PI))));
        //entity.setYaw(MathHelper.wrapDegrees((float)(MathHelper.atan2(dir.getOffsetZ(), dir.getOffsetX()) * 180.0F / (float)Math.PI) - 90.0F));
        entity.setHeadYaw(entity.getYaw());
        return entity;
    }

    protected float getDrag() {
        return 0.73f;
    }

    public boolean isOnFire() {
        return false;
    }

    public float getEffectiveExplosionResistance(Explosion explosion, BlockView world, BlockPos pos, BlockState blockState, FluidState fluidState, float max) {
        return max;
    }

    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (!this.getWorld().isClient) {
            Entity target = entityHitResult.getEntity();
            Entity attacker = this.getOwner();
            if (attacker instanceof LivingEntity livingEntity) {
                var bl = target.damage(this.getDamageSources().create(DamageTypes.WITHER_SKULL, this, attacker), 3.0F);
                if (bl) {
                    if (target.isAlive()) {
                        //this.applyDamageEffects(livingEntity, target);
                    } else {
                        livingEntity.heal(3.0F);
                    }
                }
            } else {
                target.damage(this.getDamageSources().magic(), 3.0F);
            }
        }
    }

    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 0.6f, false, ExplosionSourceType.TNT);
            this.discard();
        }
    }

    public boolean canHit() {
        return false;
    }

    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    protected void initDataTracker() {
    }

    protected boolean isBurning() {
        return false;
    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.WITHER_SKULL;
    }
}
