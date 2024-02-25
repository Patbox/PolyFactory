package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import eu.pb4.polyfactory.mixin.FallingBlockAccessor;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.joml.Vector3f;

import java.util.List;

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
        var fluidState = Fluids.EMPTY.getDefaultState();
        while ((!testState.isSideSolidFullSquare(world, mut, dir) || testState.isIn(FactoryBlockTags.WIND_PASSTHROUGH)) && length < speed * 20 * Math.pow(0.98, length)) {
            mut.move(dir);
            testState = world.getBlockState(mut);

            if (fluidState != testState.getFluidState()) {
                var old = fluidState;
                fluidState = testState.getFluidState();

                ParticleEffect effect = null;

                var airFluid = reverse ? old : fluidState;
                var liquidFluid = reverse ? fluidState : old;

                var pSpeed = 0d;

                if (airFluid.isOf(Fluids.EMPTY) && liquidFluid.isIn(FluidTags.WATER)) {
                    effect = new ItemStackParticleEffect(ParticleTypes.ITEM, Items.BLUE_STAINED_GLASS_PANE.getDefaultStack());
                    pSpeed = speed * 0.8f;
                } else if (airFluid.isOf(Fluids.EMPTY) && liquidFluid.isIn(FluidTags.LAVA)) {
                    effect = ParticleTypes.FLAME;
                    pSpeed = speed * 0.2f;
                }

                if (effect != null && (world.getTime() + pos.getX() * 3L + pos.getY() * 7L + pos.getZ() * 5L) % MathHelper.clamp(Math.round(2 / speed), 4, 8) == 0) {
                    var a = Vec3d.ofCenter(mut).offset(dir, -0.5f);
                    ((ServerWorld) world).spawnParticles(effect, a.x + world.random.nextFloat() - 0.5, a.y + world.random.nextFloat() - 0.5, a.z + world.random.nextFloat() - 0.5, 0, dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ(), reverse ? -pSpeed : pSpeed);
                }
            }

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

            var original = entity.getVelocity();
            var base = entity.getVelocity().getComponentAlongAxis(dir.getAxis());
            entity.setVelocity(entity.getVelocity().withAxis(dir.getAxis(), base * 0.8f).add(Vec3d.of(dir.getVector()).multiply(x)));

            if (dir.getAxis() == Direction.Axis.Y && dir.getDirection() == Direction.AxisDirection.NEGATIVE == reverse) {
                entity.fallDistance = 0;
            }

            if (entity instanceof ServerPlayerEntity player) {
                FactoryUtil.sendVelocityDelta(player, entity.getVelocity().subtract(original));

                ((ServerPlayNetExt) player.networkHandler).polyFactory$resetFloating();
                if (x > 0.05 || x < -0.05) {
                    //entity.velocityModified = true;
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
