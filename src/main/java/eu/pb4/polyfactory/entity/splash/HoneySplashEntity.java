package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.effects.FactoryEffects;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
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

public class HoneySplashEntity extends SplashEntity<Unit> {
    private static final ParticleEffect PARTICLE = new ItemStackParticleEffect(ParticleTypes.ITEM, Items.HONEY_BLOCK.getDefaultStack());

    public HoneySplashEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world, FactoryFluids.HONEY);
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getEntityWorld().isClient()) {
            Direction direction = blockHitResult.getSide();
            BlockPos targetBlockPos = blockHitResult.getBlockPos();
            BlockPos sidePos = targetBlockPos.offset(direction);
            this.extinguishFire(sidePos);
            this.extinguishFire(sidePos.offset(direction.getOpposite()));

            for(Direction direction2 : Direction.Type.HORIZONTAL) {
                this.extinguishFire(sidePos.offset(direction2));
            }
        }
        super.onBlockHit(blockHitResult);
    }
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.3) {
            var entity = entityHitResult.getEntity();

            if (getEntityWorld() instanceof ServerWorld && entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                if (livingEntity.isOnFire() && livingEntity.isAlive() && this.canInteractEntity(entity)) {
                    livingEntity.extinguishWithSound();
                }
                var effect = livingEntity.getStatusEffect(FactoryEffects.STICKY_HONEY);
                int time = 20;
                if (effect != null) {
                    time += effect.getDuration();
                }
                livingEntity.addStatusEffect(new StatusEffectInstance(FactoryEffects.STICKY_HONEY, Math.min(time, 20 * 60), 0), this);            }
        }
        super.onEntityHit(entityHitResult);
    }

    @Override
    public ParticleEffect getBaseParticle() {
        return PARTICLE;
    }

    private void extinguishFire(BlockPos pos) {
        if (this.random.nextFloat() < 0.1) {
            if (!this.canBreakBlock(pos)) {
                return;
            }

            BlockState blockState = this.getEntityWorld().getBlockState(pos);
            if (blockState.isIn(BlockTags.FIRE)) {
                this.getEntityWorld().breakBlock(pos, false, this);
            } else if (AbstractCandleBlock.isLitCandle(blockState)) {
                AbstractCandleBlock.extinguish(null, blockState, this.getEntityWorld(), pos);
            } else if (CampfireBlock.isLitCampfire(blockState)) {
                this.getEntityWorld().syncWorldEvent(null, WorldEvents.FIRE_EXTINGUISHED, pos, 0);
                CampfireBlock.extinguish(this.getOwner(), this.getEntityWorld(), pos, blockState);
                this.getEntityWorld().setBlockState(pos, blockState.with(CampfireBlock.LIT, false));
            }
        }
    }
}
