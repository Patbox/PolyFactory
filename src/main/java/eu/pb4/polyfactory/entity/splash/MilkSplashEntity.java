package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.mixin.MobEffectInstanceAccessor;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class MilkSplashEntity extends SplashEntity<Unit> {
    public MilkSplashEntity(EntityType<MilkSplashEntity> entityType, Level world) {
        super(entityType, world, FactoryFluids.MILK);
    }
    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
    }
    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.4 && (entityHitResult.getEntity() instanceof LivingEntity livingEntity)) {
            if (!this.canInteractEntity(livingEntity)) {
                return;
            }
            for (var effect : livingEntity.getActiveEffects()) {
                effect = new MobEffectInstance(effect);
                ((MobEffectInstanceAccessor) effect).setDuration(effect.getDuration() - 20);
                livingEntity.forceAddEffect(effect, this);
            }
        }
        super.onHitEntity(entityHitResult);
    }
}
