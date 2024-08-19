package eu.pb4.polyfactory.fluid.shooting;

import eu.pb4.polyfactory.entity.splash.SplashEntity;
import eu.pb4.polyfactory.entity.splash.WaterSplashEntity;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.mixin.ProjectileEntityAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Unit;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.function.BiFunction;

public record ShootSplashed<T>(BiFunction<ServerWorld, FluidInstance<T>, SplashEntity<T>> entityCreator, int splashPerTick, long amount, RegistryEntry<SoundEvent> soundEvent)
            implements FluidShootingBehavior<T> {

    public static <T> FluidShootingBehavior<T> of(EntityType<? extends SplashEntity<T>> entityType, long amount, RegistryEntry<SoundEvent> soundEvent) {
        return new ShootSplashed<>((world, fluid) -> entityType.create(world), 10, amount, soundEvent);
    }

    @Override
    public boolean canShoot(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container) {
        return container.get(fluidInstance) >= amount;
    }

    @Override
    public void startShooting(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container) {
        shoot(context, 0.1f, fluidInstance, container);
    }

    @Override
    public void continueShooting(ShooterContext context, FluidInstance<T> fluidInstance, int tick, FluidContainer container) {
        shoot(context, Math.min(0.1f + (tick) / 100f, 0.3f), fluidInstance, container);
    }

    private void shoot(ShooterContext context, float divergence, FluidInstance<T> fluidInstance, FluidContainer container) {
        container.extract(fluidInstance, amount, false);
        var vec = new Vector3f();
        var pos = context.position();
        var rotation = context.rotation();
        var random = context.random();
        var world = context.world();
        for (int i = 0; i < this.splashPerTick; i++) {
            var projectile = entityCreator.apply(world, fluidInstance);
            assert projectile != null;
            ((ProjectileEntityAccessor) projectile).setOwnerUuid(context.uuid());
            projectile.setPosition(pos);
            vec.set(rotation.x, rotation.y, rotation.z);
            vec.add((float) random.nextTriangular(0, divergence),
                    (float) random.nextTriangular(0, divergence),
                    (float) random.nextTriangular(0, divergence));
            vec.normalize();
            vec.mul(2.0f + random.nextFloat() * 0.5f);

            projectile.setVelocity(vec.x, vec.y, vec.z);
            world.spawnEntity(projectile);
        }
        world.playSound(null, pos.x, pos.y, pos.z, this.soundEvent, context.soundCategory(), 1, (float) random.nextTriangular(1, 0.1));
    }

    @Override
    public void stopShooting(ShooterContext context, FluidInstance<T> fluidInstance) {}
}