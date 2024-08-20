package eu.pb4.polyfactory.fluid.shooting;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.mixin.ProjectileEntityAccessor;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.sound.SoundEvents;
import org.joml.Vector3f;

import java.util.function.BiFunction;

public record ShootSnowball<T>(long amount)
            implements FluidShootingBehavior<T> {
    @Override
    public boolean canShoot(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container) {
        return container.get(fluidInstance) >= amount;
    }

    @Override
    public void startShooting(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container) {
        shoot(context, 0f, fluidInstance, container);
    }

    @Override
    public void continueShooting(ShooterContext context, FluidInstance<T> fluidInstance, int tick, FluidContainer container) {
        shoot(context, 0.1f, fluidInstance, container);
    }

    private void shoot(ShooterContext context, float divergence, FluidInstance<T> fluidInstance, FluidContainer container) {
        container.extract(fluidInstance, amount, false);
        var vec = new Vector3f();
        var pos = context.position();
        var rotation = context.rotation();
        var random = context.random();
        var world = context.world();
        var projectile = new SnowballEntity(world, pos.x, pos.y, pos.z);
        ((ProjectileEntityAccessor) projectile).setOwnerUuid(context.uuid());
        vec.set(rotation.x, rotation.y, rotation.z);
        vec.add((float) random.nextTriangular(0, divergence),
                (float) random.nextTriangular(0, divergence),
                (float) random.nextTriangular(0, divergence));
        vec.normalize();
        vec.mul(1.7f + random.nextFloat() * 0.5f);

        projectile.setVelocity(vec.x, vec.y, vec.z);
        world.spawnEntity(projectile);

        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_SNOWBALL_THROW, context.soundCategory(), 1, (float) random.nextTriangular(1, 0.1));
    }

    @Override
    public void stopShooting(ShooterContext context, FluidInstance<T> fluidInstance) {}
}