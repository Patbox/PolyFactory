package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.joml.Vector3f;

public class WaterSplashEntity extends SplashEntity<Unit> {
    private static final int WATER_COLOR = -13083194;
    //private static final ParticleEffect PARTICLE = EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, WATER_COLOR);
    private static final ParticleEffect PARTICLE = new ItemStackParticleEffect(ParticleTypes.ITEM, Items.BLUE_STAINED_GLASS_PANE.asItem().getDefaultStack());
    //private static final ParticleEffect PARTICLE = new DustParticleEffect(new Vector3f(56 / 255f, 93/ 255f, 199/ 255f), 0.5f);
    public WaterSplashEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world, FactoryFluids.WATER);
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getWorld().isClient && this.random.nextFloat() < 0.3) {
            Direction direction = blockHitResult.getSide();
            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockPos blockPos2 = blockPos.offset(direction);
            this.extinguishFire(blockPos2);
            this.extinguishFire(blockPos2.offset(direction.getOpposite()));

            for(Direction direction2 : Direction.Type.HORIZONTAL) {
                this.extinguishFire(blockPos2.offset(direction2));
            }
        }
        super.onBlockHit(blockHitResult);
    }
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.3) {
            var entity = entityHitResult.getEntity();

            if (entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                if (livingEntity.hurtByWater()) {
                    livingEntity.damage(this.getDamageSources().indirectMagic(this, this.getOwner()), 0.6F);
                }

                if (livingEntity.isOnFire() && livingEntity.isAlive()) {
                    livingEntity.extinguishWithSound();
                }
            }

            if (entity instanceof AxolotlEntity axolotlEntity) {
                axolotlEntity.hydrateFromPotion();
            }
        }
        super.onEntityHit(entityHitResult);
    }

    @Override
    public ParticleEffect getBaseParticle() {
        return PARTICLE;
    }

    @Override
    protected double getParticleSpeed() {
        return super.getParticleSpeed() * 2;
    }

    @Override
    protected double getParticleCollisionSpeed() {
        return super.getParticleCollisionSpeed() * 2;
    }

    private void extinguishFire(BlockPos pos) {
        if (this.random.nextFloat() < 0.3) {
            BlockState blockState = this.getWorld().getBlockState(pos);
            if (blockState.isIn(BlockTags.FIRE)) {
                this.getWorld().breakBlock(pos, false, this);
            } else if (AbstractCandleBlock.isLitCandle(blockState)) {
                AbstractCandleBlock.extinguish(null, blockState, this.getWorld(), pos);
            } else if (CampfireBlock.isLitCampfire(blockState)) {
                this.getWorld().syncWorldEvent(null, WorldEvents.FIRE_EXTINGUISHED, pos, 0);
                CampfireBlock.extinguish(this.getOwner(), this.getWorld(), pos, blockState);
                this.getWorld().setBlockState(pos, blockState.with(CampfireBlock.LIT, false));
            }
        }
    }
}
