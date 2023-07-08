package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
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
    private Box collisionBox;
    private Direction cachedDirection;
    private Vec3d center;

    public FanBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FAN, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!state.get(FanBlock.ENABLED)) {
            return;
        }
        var self = (FanBlockEntity) t;

        var speed = RotationUser.getRotation((ServerWorld) world, pos).speed() * MathHelper.RADIANS_PER_DEGREE;

        if (speed == 0) {
            return;
        }

        if (self.collisionBox == null || self.cachedDirection != state.get(FanBlock.FACING)) {
            self.cachedDirection = state.get(FanBlock.FACING);
            self.collisionBox = new Box(Vec3d.of(pos), Vec3d.of(pos.offset(self.cachedDirection, 32).add(1, 1, 1)));
            self.center = Vec3d.ofCenter(pos);
        }

        if (world.getTime() % MathHelper.clamp(Math.round(2 / speed), 2, 4) == 0) {
            var a = Vec3d.ofCenter(pos).offset(self.cachedDirection, 1f);
            ((ServerWorld) world).spawnParticles(ParticleTypes.CLOUD, a.x + world.random.nextFloat() - 0.5, a.y + world.random.nextFloat() - 0.5, a.z + world.random.nextFloat() - 0.5, 0, self.cachedDirection.getOffsetX(), self.cachedDirection.getOffsetY(), self.cachedDirection.getOffsetZ(), speed);
        }

        for (var entity : world.getEntitiesByClass(Entity.class, self.collisionBox, EntityPredicates.EXCEPT_SPECTATOR)) {
            var l = entity.getBoundingBox().getCenter().squaredDistanceTo(self.center);
            var x = speed * Math.pow(0.98, l);
            if (x > 0.05) {
                var base = entity.getVelocity().getComponentAlongAxis(self.cachedDirection.getAxis());
                entity.setVelocity(entity.getVelocity().withAxis(self.cachedDirection.getAxis(), base * 0.8f).add(Vec3d.of(self.cachedDirection.getVector()).multiply(x)));

                if (self.cachedDirection == Direction.UP) {
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
