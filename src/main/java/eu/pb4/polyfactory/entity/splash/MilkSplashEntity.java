package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.mixin.StatusEffectInstanceAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class MilkSplashEntity extends SplashEntity<Unit> {
    public MilkSplashEntity(EntityType<MilkSplashEntity> entityType, World world) {
        super(entityType, world, FactoryFluids.MILK);
    }
    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
    }
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.4 && (entityHitResult.getEntity() instanceof LivingEntity livingEntity)) {
            if (!this.canInteractEntity(livingEntity)) {
                return;
            }
            for (var effect : livingEntity.getStatusEffects()) {
                effect = new StatusEffectInstance(effect);
                ((StatusEffectInstanceAccessor) effect).setDuration(effect.getDuration() - 20);
                livingEntity.setStatusEffect(effect, this);
            }
        }
        super.onEntityHit(entityHitResult);
    }
}
