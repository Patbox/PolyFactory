package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import eu.pb4.polyfactory.mixin.FallingBlockAccessor;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public class FanBlockEntity extends BlockEntity {

    public FanBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FAN, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!state.get(FanBlock.ENABLED)) {
            return;
        }

        var reverse = state.get(FanBlock.REVERSE);
        var speed = RotationUser.getRotation(world, pos).speed() * MathHelper.RADIANS_PER_DEGREE * 2;

        if (speed == 0) {
            return;
        }

        var dir = state.get(FanBlock.FACING);
        var mut = pos.mutableCopy().move(dir);
        int length = 0;

        var testState = world.getBlockState(mut);
        while ((!testState.isSideSolidFullSquare(world, mut, dir) || testState.isIn(FactoryBlockTags.WIND_PASSTHROUGH)) && length < speed * 20 * Math.pow(0.98, length)) {
            mut.move(dir);
            testState = world.getBlockState(mut);
            length++;
        }

        if (testState.getBlock() instanceof FallingBlock fallingBlock && speed * length / (length + 1) * Math.pow(0.98, length) > 0.05) {
            var fallingBlockEntity = FallingBlockEntity.spawnFromBlock(world, mut.toImmutable(), testState);
            ((FallingBlockAccessor) fallingBlock).callConfigureFallingBlockEntity(fallingBlockEntity);
            if (fallingBlockEntity instanceof LastFanEffectedTickConsumer consumer) {
                consumer.polyfactory$setLastFanTick();
            }
        }

        if (length == 0) {
            return;
        }

        mut.move(dir.getOpposite());

        var center = Vec3d.ofCenter(pos);

        var box = new Box(Math.min(pos.getX(), mut.getX()), Math.min(pos.getY(), mut.getY()), Math.min(pos.getZ(), mut.getZ()), Math.max(pos.getX(), mut.getX()) + 1, Math.max(pos.getY(), mut.getY()) + 1, Math.max(pos.getZ(), mut.getZ()) + 1);

        if ((world.getTime() + pos.getX() * 3L + pos.getY() * 7L + pos.getZ() * 5L) % MathHelper.clamp(Math.round(2 / speed), 8, 16) == 0) {
            var a = center.offset(dir, 1f);
            if (reverse) {
                a = a.offset(dir, length);
                var alt = Vec3d.ofCenter(mut);
                a = center.squaredDistanceTo(a) < center.squaredDistanceTo(alt) ? a : alt;
            }
            ((ServerWorld) world).spawnParticles(ParticleTypes.CLOUD, a.x + world.random.nextFloat() - 0.5, a.y + world.random.nextFloat() - 0.5, a.z + world.random.nextFloat() - 0.5, 0, dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ(), reverse ? -speed : speed);
        }

        for (var entity : world.getEntitiesByClass(Entity.class, box, EntityPredicates.EXCEPT_SPECTATOR)) {
            if (entity instanceof LivingEntity livingEntity
                    && EnchantmentHelper.getEquipmentLevel(FactoryEnchantments.IGNORE_MOVEMENT, livingEntity) != 0) {
                continue;
            }

            if (entity instanceof LastFanEffectedTickConsumer consumer) {
                consumer.polyfactory$setLastFanTick();
            }

            var l = entity.getBoundingBox().getCenter().distanceTo(center);
            var x = speed * (length - l) / (length + 1) * Math.pow(0.98, length);
            if (reverse) {
                x = -x;
            }

            var base = entity.getVelocity().getComponentAlongAxis(dir.getAxis());
            entity.setVelocity(entity.getVelocity().withAxis(dir.getAxis(), base * 0.8f).add(Vec3d.of(dir.getVector()).multiply(x)));

            if (dir.getAxis() == Direction.Axis.Y && dir.getDirection() == Direction.AxisDirection.NEGATIVE == reverse) {
                entity.fallDistance = 0;
            }

            if (entity instanceof ServerPlayerEntity player) {
                ((ServerPlayNetExt) player.networkHandler).polyFactory$resetFloating();
                if (x > 0.05 || x < -0.05) {
                    entity.velocityModified = true;
                    TriggerCriterion.trigger(player, FactoryTriggers.MOVED_BY_FAN);
                    var alt = (LastFanEffectedTickConsumer) player;
                    if (dir == Direction.UP && !player.isOnGround() && player.age - alt.polyfactory$getLastOnGround() > 10 && player.getY() - alt.polyfactory$getLastY() > 32 && player.getVelocity().getY() > 0.5) {
                        TriggerCriterion.trigger(player, FactoryTriggers.MOVED_BY_FAN_A_LOT);
                    }
                }
            } else {
                entity.velocityModified = true;
            }
        }
    }
}
