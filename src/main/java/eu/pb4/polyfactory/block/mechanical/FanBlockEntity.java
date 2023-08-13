package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.util.FactoryEntityTags;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
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

        var speed = RotationUser.getRotation((ServerWorld) world, pos).speed() * MathHelper.RADIANS_PER_DEGREE;

        if (speed == 0) {
            return;
        }

        var dir = state.get(FanBlock.FACING);
        var mut = pos.mutableCopy().move(dir);
        int length = 0;

        var testState = world.getBlockState(mut);
        while ((!testState.isSideSolidFullSquare(world, mut, dir) || testState.isIn(FactoryBlockTags.WIND_PASSTHROUGH)) && length < 32) {
            mut.move(dir);
            testState = world.getBlockState(mut);
            length++;
        }
        if (length == 0) {
            return;
        }

        mut.move(dir.getOpposite());

        var center = Vec3d.ofCenter(pos);

        var box = new Box(Math.min(pos.getX(), mut.getX()), Math.min(pos.getY(), mut.getY()), Math.min(pos.getZ(), mut.getZ()), Math.max(pos.getX(), mut.getX()) + 1, Math.max(pos.getY(), mut.getY()) + 1, Math.max(pos.getZ(), mut.getZ()) + 1);

        if ((world.getTime() + pos.getX() * 3 + pos.getY() * 7 + pos.getZ() * 5) % MathHelper.clamp(Math.round(2 / speed), 8, 16) == 0) {
            var a = Vec3d.ofCenter(pos).offset(dir, 1f);
            ((ServerWorld) world).spawnParticles(ParticleTypes.CLOUD, a.x + world.random.nextFloat() - 0.5, a.y + world.random.nextFloat() - 0.5, a.z + world.random.nextFloat() - 0.5, 0, dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ(), speed);
        }

        for (var entity : world.getEntitiesByClass(Entity.class, box, EntityPredicates.EXCEPT_SPECTATOR)) {
            var l = entity.getBoundingBox().getCenter().squaredDistanceTo(center);
            var x = speed * Math.pow(0.98, l);
            if (x > 0.05) {
                var base = entity.getVelocity().getComponentAlongAxis(dir.getAxis());
                entity.setVelocity(entity.getVelocity().withAxis(dir.getAxis(), base * 0.8f).add(Vec3d.of(dir.getVector()).multiply(x)));

                if (dir == Direction.UP) {
                    entity.fallDistance = 0;
                }

                if (entity instanceof ServerPlayerEntity player) {
                    if (player.age % 2 == 0) {
                        entity.velocityModified = true;
                    }
                    ((ServerPlayNetExt) player.networkHandler).polyFactory$resetFloating();
                } else {
                    entity.velocityModified = true;
                }
            }
        }
    }
}
