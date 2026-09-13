package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.BlockHitResult;

public class FertilizerSplashEntity extends SplashEntity<Unit> {
    public FertilizerSplashEntity(EntityType<? extends Projectile> entityType, Level world) {
        super(entityType, world, FactoryFluids.FERTILIZER);
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (!this.level().isClientSide()) {
            Direction direction = blockHitResult.getDirection();
            BlockPos targetBlockPos = blockHitResult.getBlockPos();
            BlockPos sidePos = targetBlockPos.relative(direction);

            this.applyFertilizer(sidePos);
            this.applyFertilizer(sidePos.relative(direction.getOpposite()));
        }
        super.onHitBlock(blockHitResult);
    }
    /*@Override
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
    }*/

    private void applyFertilizer(BlockPos pos) {
        if (this.random.nextFloat() < 0.25 && this.level() instanceof ServerLevel level) {
            if (!this.canBreakBlock(pos)) {
                return;
            }
            var state = this.level().getBlockState(pos);

            if (state.getBlock() instanceof BonemealableBlock bonemealableBlock && bonemealableBlock.isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION)) {
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
                if (bonemealableBlock.isBonemealSuccess(level, this.random, pos, state, BonemealSource.INTERACTION)) {
                    bonemealableBlock.performBonemeal(level, this.random, pos, state, BonemealSource.INTERACTION);
                }
            }
        }
    }
}
