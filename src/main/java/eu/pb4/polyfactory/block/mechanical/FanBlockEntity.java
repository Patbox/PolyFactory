package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.item.FactoryEnchantmentEffectComponents;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import eu.pb4.polyfactory.mixin.FallingBlockAccessor;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FanBlockEntity extends BlockEntity {

    public FanBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FAN, pos, state);
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        if (!state.getValue(FanBlock.ENABLED)) {
            return;
        }

        var reverse = state.getValue(FanBlock.REVERSE);
        var speed = Math.min(RotationUser.getRotation(world, pos).speed() * Mth.DEG_TO_RAD * 2, 1024);

        if (speed == 0) {
            return;
        }

        var dir = state.getValue(FanBlock.FACING);
        var mut = pos.mutable().move(dir);
        int length = 0;

        var testState = world.getBlockState(mut);
        var fluidState = Fluids.EMPTY.defaultFluidState();
        while ((!testState.isFaceSturdy(world, mut, dir) || testState.getCollisionShape(world, mut).isEmpty() || testState.is(FactoryBlockTags.WIND_PASSTHROUGH)) && length < speed * 20 * Math.pow(0.98, length)) {
            mut.move(dir);
            testState = world.getBlockState(mut);

            if (fluidState != testState.getFluidState()) {
                var old = fluidState;
                fluidState = testState.getFluidState();

                ParticleOptions effect = null;

                var airFluid = reverse ? old : fluidState;
                var liquidFluid = reverse ? fluidState : old;

                var pSpeed = 0d;

                if (airFluid.is(Fluids.EMPTY) && liquidFluid.is(FluidTags.WATER)) {
                    effect = new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_STAINED_GLASS_PANE.getDefaultInstance());
                    pSpeed = speed * 0.8f;
                } else if (airFluid.is(Fluids.EMPTY) && liquidFluid.is(FluidTags.LAVA)) {
                    effect = ParticleTypes.FLAME;
                    pSpeed = speed * 0.2f;
                }

                if (effect != null && (world.getGameTime() + pos.getX() * 3L + pos.getY() * 7L + pos.getZ() * 5L) % Mth.clamp(Math.round(2 / speed), 4, 8) == 0) {
                    var a = Vec3.atCenterOf(mut).relative(dir, -0.5f);
                    ((ServerLevel) world).sendParticles(effect, a.x + world.random.nextFloat() - 0.5, a.y + world.random.nextFloat() - 0.5, a.z + world.random.nextFloat() - 0.5, 0, dir.getStepX(), dir.getStepY(), dir.getStepZ(), reverse ? -pSpeed : pSpeed);
                }
            }

            length++;
        }

        if (testState.getBlock() instanceof FallingBlock fallingBlock && speed * length / (length + 1) * Math.pow(0.98, length) > 0.05) {
            var fallingBlockEntity = FallingBlockEntity.fall(world, mut.immutable(), testState);
            ((FallingBlockAccessor) fallingBlock).callFalling(fallingBlockEntity);
            if (fallingBlockEntity instanceof LastFanEffectedTickConsumer consumer) {
                consumer.polyfactory$setLastFanTick();
            }
        }

        if (length == 0) {
            return;
        }

        mut.move(dir.getOpposite());

        var center = Vec3.atCenterOf(pos);

        var box = new AABB(Math.min(pos.getX(), mut.getX()), Math.min(pos.getY(), mut.getY()), Math.min(pos.getZ(), mut.getZ()), Math.max(pos.getX(), mut.getX()) + 1, Math.max(pos.getY(), mut.getY()) + 1, Math.max(pos.getZ(), mut.getZ()) + 1);

        if ((world.getGameTime() + pos.getX() * 3L + pos.getY() * 7L + pos.getZ() * 5L) % Mth.clamp(Math.round(2 / speed), 8, 16) == 0) {
            var a = center.relative(dir, 1f);
            if (reverse) {
                a = a.relative(dir, length);
                var alt = Vec3.atCenterOf(mut);
                a = center.distanceToSqr(a) < center.distanceToSqr(alt) ? a : alt;
            }
            ((ServerLevel) world).sendParticles(ParticleTypes.CLOUD, a.x + world.random.nextFloat() - 0.5, a.y + world.random.nextFloat() - 0.5, a.z + world.random.nextFloat() - 0.5, 0, dir.getStepX(), dir.getStepY(), dir.getStepZ(), reverse ? -speed : speed);
        }

        for (var entity : world.getEntitiesOfClass(Entity.class, box, EntitySelector.NO_SPECTATORS)) {
            float mult = 1;

            if (entity instanceof LivingEntity livingEntity) {
                mult = FactoryEnchantments.getMultiplier(livingEntity, FactoryEnchantmentEffectComponents.FAN_PUSH_MULTIPLIER);
                if (mult <= 0) {
                    return;
                }
            }

            if (entity instanceof LastFanEffectedTickConsumer consumer) {
                consumer.polyfactory$setLastFanTick();
            }

            var l = entity.getBoundingBox().getCenter().distanceTo(center);
            var x = speed * (length - l) / (length + 1) * Math.pow(0.98, length) * mult;
            if (reverse) {
                x = -x;
            }

            var original = entity.getDeltaMovement();
            var base = entity.getDeltaMovement().get(dir.getAxis());
            FactoryUtil.setSafeVelocity(entity, entity.getDeltaMovement().with(dir.getAxis(), base * 0.8f).add(Vec3.atLowerCornerOf(dir.getUnitVec3i()).scale(x)));
            if (dir.getAxis() == Direction.Axis.Y && dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE == reverse) {
                entity.fallDistance = 0;
            }

            if (entity instanceof ServerPlayer player) {
                FactoryUtil.sendVelocityDelta(player, entity.getDeltaMovement().subtract(original));

                ((ServerPlayNetExt) player.connection).polyFactory$resetFloating();
                if (x > 0.05 || x < -0.05) {
                    //entity.velocityModified = true;
                    TriggerCriterion.trigger(player, FactoryTriggers.MOVED_BY_FAN);
                    var alt = (LastFanEffectedTickConsumer) player;
                    if (dir == Direction.UP && !player.onGround() && player.tickCount - alt.polyfactory$getLastOnGround() > 10 && player.getY() - alt.polyfactory$getLastY() > 32 && player.getDeltaMovement().y() > 0.5) {
                        TriggerCriterion.trigger(player, FactoryTriggers.MOVED_BY_FAN_A_LOT);
                    }
                }
            } else {
                entity.hurtMarked = true;
            }
        }
    }
}
