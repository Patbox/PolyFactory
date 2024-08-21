package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.event.GameEvent;

public class LavaSplashEntity extends SplashEntity<Unit> {
    public LavaSplashEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world, FactoryFluids.LAVA);
    }
    @Override
    protected void onBlockHit(BlockHitResult context) {
        if (!this.getWorld().isClient && this.random.nextFloat() < 0.3 && this.canBreakBlock(context.getBlockPos())) {
            var blockPos = context.getBlockPos();
            var world = this.getWorld();
            var blockState = world.getBlockState(blockPos);
            if (!CampfireBlock.canBeLit(blockState) && !CandleBlock.canBeLit(blockState) && !CandleCakeBlock.canBeLit(blockState)) {
                BlockPos blockPos2 = blockPos.offset(context.getSide());
                if (AbstractFireBlock.canPlaceAt(world, blockPos2, context.getSide())) {
                    BlockState blockState2 = AbstractFireBlock.getState(world, blockPos2);
                    world.setBlockState(blockPos2, blockState2, Block.NOTIFY_ALL_AND_REDRAW);
                    world.emitGameEvent(this, GameEvent.BLOCK_PLACE, blockPos);
                }
            } else {
                world.setBlockState(blockPos, blockState.with(Properties.LIT, true), Block.NOTIFY_ALL_AND_REDRAW);
                world.emitGameEvent(this, GameEvent.BLOCK_CHANGE, blockPos);
            }
        }
        super.onBlockHit(context);
    }
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.random.nextFloat() < 0.3) {
            if (entityHitResult.getEntity() instanceof LivingEntity livingEntity && this.canDamageEntity(livingEntity)) {
                livingEntity.setOnFireFor(3);
                livingEntity.damage(this.getDamageSources().create(DamageTypes.LAVA, this, this.getOwner()), 0.6F);
            }
        }
        super.onEntityHit(entityHitResult);
    }

    @Override
    protected boolean discardInBlock(BlockState state) {
        if (state.getFluidState().isIn(FluidTags.WATER)) {
            ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(),
                    0, 0, 0, 0, 0);
            this.playExtinguishSound();
        }

        return !state.getFluidState().isEmpty();
    }

    @Override
    public ParticleEffect getBaseParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected double getParticleSpeed() {
        return 0.12;
    }

    @Override
    protected double getParticleCollisionSpeed() {
        return 0.005;
    }
}
