package eu.pb4.polyfactory.fluid.shooting;

import eu.pb4.polyfactory.entity.splash.SplashEntity;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.mixin.ProjectileEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import org.joml.Vector3f;

public record ShootProjectileEntity<T>(EntityCreator<T> entityCreator,
                                       int splashPerTick, AmountGetter<T> amount,
                                       float baseSpeed, float extraSpeed,
                                       float initialDivergence, float maxDivergence,
                                       RegistryEntry<SoundEvent> soundEvent)
            implements FluidShootingBehavior<T> {

    public static <T> FluidShootingBehavior<T> ofSplash(EntityType<? extends SplashEntity<T>> entityType, int splashPerTick, long amount, RegistryEntry<SoundEvent> soundEvent) {
        return ofSplash(entityType, splashPerTick, amount, 2, 0.5f, 0.1f, 0.3f, soundEvent);
    }

    public static <T> FluidShootingBehavior<T> ofSplash(EntityType<? extends SplashEntity<T>> entityType, int splashPerTick, long amount, float baseSpeed, float extraSpeed,
                                                        float initialDivergence, float maxDivergence, RegistryEntry<SoundEvent> soundEvent) {
        return new ShootProjectileEntity<>((world, fluid, a) -> {
            var splash = entityType.create(world);
            assert splash != null;
            splash.setFluidData(fluid.data());
            return splash;
        }, splashPerTick, (w, a,b, c) -> amount, baseSpeed, extraSpeed, initialDivergence, maxDivergence, soundEvent);
    }

    public static <T> FluidShootingBehavior<T> ofEntity(EntityType<?> entityType, int splashPerTick, long amount, float baseSpeed, float extraSpeed,
                                                    float initialDivergence, float maxDivergence, RegistryEntry<SoundEvent> soundEvent) {
        return new ShootProjectileEntity<>((world, fluid, a) -> entityType.create(world), splashPerTick, (w, a, b, c) -> amount, baseSpeed, extraSpeed, initialDivergence, maxDivergence, soundEvent);
    }

    @Override
    public boolean canShoot(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container) {
        return container.get(fluidInstance) >= amount.getAmount(context.world(), container, fluidInstance, true);
    }

    @Override
    public void startShooting(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container) {
    }

    @Override
    public void continueShooting(ShooterContext context, FluidInstance<T> fluidInstance, int tick, FluidContainer container) {
        shoot(context, Math.min(initialDivergence + (tick) / 100f, maxDivergence), fluidInstance, container);
    }

    private void shoot(ShooterContext context, float divergence, FluidInstance<T> fluidInstance, FluidContainer container) {
        var amount = this.amount.getAmount(context.world(), container, fluidInstance, false);
        container.extract(fluidInstance, amount, false);
        var vec = new Vector3f();
        var pos = context.position();
        var rotation = context.rotation();
        var random = context.random();
        var world = context.world();
        for (int i = 0; i < this.splashPerTick; i++) {
            var entity = entityCreator.createEntity(world, fluidInstance, amount);
            if (entity instanceof ProjectileEntity projectile) {
                ((ProjectileEntityAccessor) projectile).setOwnerUuid(context.uuid());
            }
            entity.setPosition(pos);
            vec.set(rotation.x, rotation.y, rotation.z);
            vec.add((float) random.nextTriangular(0, divergence),
                    (float) random.nextTriangular(0, divergence),
                    (float) random.nextTriangular(0, divergence));
            vec.normalize();
            vec.mul((this.baseSpeed + random.nextFloat() * this.extraSpeed) * context.force());

            entity.setVelocity(vec.x, vec.y, vec.z);
            world.spawnEntity(entity);
        }
        world.playSound(null, pos.x, pos.y, pos.z, this.soundEvent, context.soundCategory(), 1, (float) random.nextTriangular(1, 0.1));
    }

    @Override
    public void stopShooting(ShooterContext context, FluidInstance<T> fluidInstance) {}


    public interface AmountGetter<T> {
        long getAmount(ServerWorld world, FluidContainer container, FluidInstance<T> instance, boolean isCheck);
    }

    public interface EntityCreator<T> {
        Entity createEntity(ServerWorld world, FluidInstance<T> fluid, long amount);
    }
}