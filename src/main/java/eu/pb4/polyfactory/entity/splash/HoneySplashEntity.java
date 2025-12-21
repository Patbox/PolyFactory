package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.effects.FactoryEffects;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class HoneySplashEntity extends SplashEntity<Unit> {
    private static final ParticleOptions PARTICLE = new ItemParticleOption(ParticleTypes.ITEM, Items.HONEY_BLOCK.getDefaultInstance());

    public HoneySplashEntity(EntityType<? extends Projectile> entityType, Level world) {
        super(entityType, world, FactoryFluids.HONEY);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (!this.level().isClientSide()) {
            Direction direction = blockHitResult.getDirection();
            BlockPos targetBlockPos = blockHitResult.getBlockPos();
            BlockPos sidePos = targetBlockPos.relative(direction);
            this.extinguishFire(sidePos);
            this.extinguishFire(sidePos.relative(direction.getOpposite()));

            for(Direction direction2 : Direction.Plane.HORIZONTAL) {
                this.extinguishFire(sidePos.relative(direction2));
            }
        }
        super.onHitBlock(blockHitResult);
    }
    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.3) {
            var entity = entityHitResult.getEntity();

            if (level() instanceof ServerLevel && entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                if (livingEntity.isOnFire() && livingEntity.isAlive() && this.canInteractEntity(entity)) {
                    livingEntity.extinguishFire();
                }
                var effect = livingEntity.getEffect(FactoryEffects.STICKY_HONEY);
                int time = 20;
                if (effect != null) {
                    time += effect.getDuration();
                }
                livingEntity.addEffect(new MobEffectInstance(FactoryEffects.STICKY_HONEY, Math.min(time, 20 * 60), 0), this);            }
        }
        super.onHitEntity(entityHitResult);
    }

    @Override
    public ParticleOptions getBaseParticle() {
        return PARTICLE;
    }

    private void extinguishFire(BlockPos pos) {
        if (this.random.nextFloat() < 0.1) {
            if (!this.canBreakBlock(pos)) {
                return;
            }

            BlockState blockState = this.level().getBlockState(pos);
            if (blockState.is(BlockTags.FIRE)) {
                this.level().destroyBlock(pos, false, this);
            } else if (AbstractCandleBlock.isLit(blockState)) {
                AbstractCandleBlock.extinguish(null, blockState, this.level(), pos);
            } else if (CampfireBlock.isLitCampfire(blockState)) {
                this.level().levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
                CampfireBlock.dowse(this.getOwner(), this.level(), pos, blockState);
                this.level().setBlockAndUpdate(pos, blockState.setValue(CampfireBlock.LIT, false));
            }
        }
    }
}
