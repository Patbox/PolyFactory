package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class LavaSplashEntity extends SplashEntity<Unit> {
    public LavaSplashEntity(EntityType<? extends Projectile> entityType, Level world) {
        super(entityType, world, FactoryFluids.LAVA);
    }
    @Override
    protected void onHitBlock(BlockHitResult context) {
        if (!this.level().isClientSide() && this.random.nextFloat() < 0.3 && this.canBreakBlock(context.getBlockPos())) {
            var blockPos = context.getBlockPos();
            var world = this.level();
            var blockState = world.getBlockState(blockPos);
            if (!CampfireBlock.canLight(blockState) && !CandleBlock.canLight(blockState) && !CandleCakeBlock.canLight(blockState)) {
                BlockPos blockPos2 = blockPos.relative(context.getDirection());
                if (BaseFireBlock.canBePlacedAt(world, blockPos2, context.getDirection())) {
                    BlockState blockState2 = BaseFireBlock.getState(world, blockPos2);
                    world.setBlock(blockPos2, blockState2, Block.UPDATE_ALL_IMMEDIATE);
                    world.gameEvent(this, GameEvent.BLOCK_PLACE, blockPos);
                }
            } else {
                world.setBlock(blockPos, blockState.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                world.gameEvent(this, GameEvent.BLOCK_CHANGE, blockPos);
            }
        }
        super.onHitBlock(context);
    }
    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.3) {
            if (this.level() instanceof ServerLevel world && entityHitResult.getEntity() instanceof LivingEntity livingEntity && this.canDamageEntity(livingEntity)) {
                livingEntity.igniteForSeconds(4);
                livingEntity.hurtServer(world, this.damageSources().source(DamageTypes.LAVA, this, this.getOwner()), 4F);
            }
        }
        super.onHitEntity(entityHitResult);
    }

    @Override
    protected boolean discardInBlock(BlockState state, BlockPos blockPos) {
        if (state.getFluidState().is(FluidTags.WATER)) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(),
                    0, 0, 0, 0, 0);
            this.playEntityOnFireExtinguishedSound();
        } else if (state.is(Blocks.POWDER_SNOW)) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(),
                    0, 0, 0, 0, 0);
            this.playEntityOnFireExtinguishedSound();
            this.level().setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
            return true;
        }

        return !state.getFluidState().isEmpty();
    }

    @Override
    public ParticleOptions getBaseParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected double getParticleSpeed() {
        return 0.12;
    }

    @Override
    protected double getParticleCollisionSpeed() {
        return 0.008;
    }
}
