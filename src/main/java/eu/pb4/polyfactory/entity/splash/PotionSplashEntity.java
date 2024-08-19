package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

import java.util.ArrayList;
import java.util.List;

public class PotionSplashEntity extends SplashEntity<PotionContentsComponent> {
    private final List<ParticleEffect> particles = new ArrayList<>();
    private final List<StatusEffectInstance> effectInstances = new ArrayList<>();

    public PotionSplashEntity(EntityType<PotionSplashEntity> entityType, World world) {
        super(entityType, world, FactoryFluids.POTION);
    }
    public static PotionSplashEntity of(ServerWorld world, FluidInstance<PotionContentsComponent> fluid) {
        var potion = new PotionSplashEntity(FactoryEntities.POTION_SPLASH, world);
        potion.setData(fluid.data());
        return potion;
    }

    @Override
    public void setData(PotionContentsComponent data) {
        super.setData(data);
        this.particles.clear();
        if (!data.hasEffects()) {
            this.particles.add(EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, -13083194));
            return;
        }
        for (var effect : data.getEffects()) {
            this.particles.add(effect.createParticle());
            if (effect.getEffectType().value().isInstant()) {
                this.effectInstances.add(effect);
            } else {
                this.effectInstances.add(new StatusEffectInstance(effect.getEffectType(), effect.getDuration() / 20, effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()));
            }
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
    }
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.4 && (entityHitResult.getEntity() instanceof LivingEntity livingEntity)) {
            for (var effect : this.effectInstances) {
                livingEntity.addStatusEffect(effect, this);
            }
        }
        super.onEntityHit(entityHitResult);
    }

    @Override
    public ParticleEffect getBaseParticle() {
        return this.particles.get(this.random.nextInt(this.particles.size()));
    }

    @Override
    protected double getParticleSpeed() {
        return super.getParticleSpeed() * 2;
    }

    @Override
    protected double getParticleCollisionSpeed() {
        return super.getParticleCollisionSpeed() * 2;
    }
}
