package eu.pb4.polyfactory.entity.splash;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import net.minecraft.block.AbstractCandleBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
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
        if (!this.getWorld().isClient) {
            Direction direction = blockHitResult.getSide();
            BlockPos targetBlockPos = blockHitResult.getBlockPos();
            var state = getWorld().getBlockState(targetBlockPos);
            if (state.getBlock() instanceof FarmlandBlock && random.nextFloat() < 0.1) {
                getWorld().setBlockState(targetBlockPos, state.with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
            }

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

            if (getWorld() instanceof ServerWorld world && entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                if (livingEntity.hurtByWater() && this.canDamageEntity(entity)) {
                    livingEntity.damage(world, this.getDamageSources().indirectMagic(this, this.getOwner()), 1F);
                }

                if (livingEntity.isOnFire() && livingEntity.isAlive() && this.canInteractEntity(entity)) {
                    livingEntity.extinguishWithSound();
                }
            }

            if (entity instanceof AxolotlEntity axolotlEntity && this.canInteractEntity(entity)) {
                axolotlEntity.hydrateFromPotion();
            }
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
