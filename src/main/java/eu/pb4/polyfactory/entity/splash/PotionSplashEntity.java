package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.mixin.MobEffectInstanceAccessor;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import java.util.ArrayList;
import java.util.List;

public class PotionSplashEntity extends SplashEntity<PotionContents> {
    private final List<ParticleOptions> particles = new ArrayList<>();
    private final List<MobEffectInstance> effectInstances = new ArrayList<>();

    public PotionSplashEntity(EntityType<PotionSplashEntity> entityType, Level world) {
        super(entityType, world, FactoryFluids.POTION);
    }
    public static PotionSplashEntity of(ServerLevel world, FluidInstance<PotionContents> fluid) {
        var potion = new PotionSplashEntity(FactoryEntities.POTION_SPLASH, world);
        potion.setFluidData(fluid.data());
        return potion;
    }

    @Override
    public void setFluidData(PotionContents data) {
        super.setFluidData(data);
        this.particles.clear();
        if (!data.hasEffects()) {
            this.particles.add(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -13083194));
            return;
        }
        for (var effect : data.getAllEffects()) {
            this.particles.add(effect.getParticleOptions());
            this.effectInstances.add(effect);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
    }
    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        if (this.level() instanceof ServerLevel world && this.random.nextFloat() < 0.8 && (entityHitResult.getEntity() instanceof LivingEntity livingEntity)) {
            for (var effect : this.effectInstances) {
                if (!this.canInteractEntity(livingEntity) && effect.getEffect().value().isBeneficial()) {
                    continue;
                } else if (!this.canDamageEntity(livingEntity) && !effect.getEffect().value().isBeneficial()) {
                    continue;
                }

                if (effect.getEffect().value().isInstantenous()) {
                    effect.getEffect().value().applyInstantenousEffect(world, this, this.getOwner(), livingEntity, effect.getAmplifier(), 0.1);
                    continue;
                }

                var current = livingEntity.getEffect(effect.getEffect());
                if (current == null) {
                    var newEffect = new MobEffectInstance(effect);
                    ((MobEffectInstanceAccessor) newEffect).setDuration(effect.getDuration() / 60);
                    livingEntity.addEffect(newEffect, this);
                } else if (current.getAmplifier() < effect.getAmplifier()) {
                    var newEffect = new MobEffectInstance(effect);
                    ((MobEffectInstanceAccessor) newEffect).setDuration(effect.getDuration() / 60);
                    ((MobEffectInstanceAccessor) newEffect).setHiddenEffect(current);
                    livingEntity.forceAddEffect(newEffect, this);
                } else if (current.getDuration() < effect.getDuration() && current.getAmplifier() == effect.getAmplifier()) {
                    var newEffect = new MobEffectInstance(current);
                    ((MobEffectInstanceAccessor) newEffect).setDuration(Math.min(effect.getDuration() / 60 + current.getDuration(), effect.getDuration()));
                    livingEntity.forceAddEffect(newEffect, this);
                }
            }
        }
        super.onHitEntity(entityHitResult);
    }

    @Override
    public ParticleOptions getBaseParticle() {
        return Util.getRandomSafe(this.particles, random).orElse(ParticleTypes.SPLASH);
    }
}
