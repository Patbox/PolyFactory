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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Vector3f;

public class WaterSplashEntity extends SplashEntity<Unit> {
    private static final int WATER_COLOR = -13083194;
    //private static final ParticleEffect PARTICLE = EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, WATER_COLOR);
    private static final ParticleOptions PARTICLE = new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_STAINED_GLASS_PANE.asItem().getDefaultInstance());
    //private static final ParticleEffect PARTICLE = new DustParticleEffect(new Vector3f(56 / 255f, 93/ 255f, 199/ 255f), 0.5f);
    public WaterSplashEntity(EntityType<? extends Projectile> entityType, Level world) {
        super(entityType, world, FactoryFluids.WATER);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (!this.level().isClientSide()) {
            Direction direction = blockHitResult.getDirection();
            BlockPos targetBlockPos = blockHitResult.getBlockPos();
            var state = level().getBlockState(targetBlockPos);
            if (state.getBlock() instanceof FarmBlock && random.nextFloat() < 0.1) {
                level().setBlockAndUpdate(targetBlockPos, state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
            }

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

            if (level() instanceof ServerLevel world && entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                if (livingEntity.isSensitiveToWater() && this.canDamageEntity(entity)) {
                    livingEntity.hurtServer(world, this.damageSources().indirectMagic(this, this.getOwner()), 1F);
                }

                if (livingEntity.isOnFire() && livingEntity.isAlive() && this.canInteractEntity(entity)) {
                    livingEntity.extinguishFire();
                }

                livingEntity.removeEffect(FactoryEffects.STICKY_SLIME);
                livingEntity.removeEffect(FactoryEffects.STICKY_HONEY);
            }


            if (entity instanceof Axolotl axolotlEntity && this.canInteractEntity(entity)) {
                axolotlEntity.rehydrate();
            }
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
