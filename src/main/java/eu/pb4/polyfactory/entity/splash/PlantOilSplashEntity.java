package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.effects.FactoryEffects;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class PlantOilSplashEntity extends SplashEntity<Unit> {
    public PlantOilSplashEntity(EntityType<? extends Projectile> entityType, Level world) {
        super(entityType, world, FactoryFluids.BIODIESEL);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.3) {
            var entity = entityHitResult.getEntity();

            if (level() instanceof ServerLevel && entity instanceof LivingEntity livingEntity && this.canInteractEntity(entity)) {
                var effect = livingEntity.getEffect(FactoryEffects.SLIPPERY);
                int time = 20;
                if (effect != null) {
                    time += effect.getDuration();
                }
                livingEntity.addEffect(new MobEffectInstance(FactoryEffects.SLIPPERY, Math.min(time, 20 * 60), 0), this);
            }
        }
        super.onHitEntity(entityHitResult);
    }
}
