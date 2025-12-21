package eu.pb4.polyfactory.fluid.shooting;

import eu.pb4.polyfactory.entity.splash.SplashEntity;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.mixin.ProjectileAccessor;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import org.joml.Vector3f;

public record ShootProjectileEntity<T>(EntityCreator<T> entityCreator,
                                       int splashPerTick, AmountGetter<T> amount,
                                       float baseSpeed, float extraSpeed,
                                       float initialDivergence, float maxDivergence,
                                       Holder<SoundEvent> soundEvent)
            implements FluidShootingBehavior<T> {

    public static <T> FluidShootingBehavior<T> ofSplash(EntityType<? extends SplashEntity<T>> entityType, int splashPerTick, long amount, Holder<SoundEvent> soundEvent) {
        return ofSplash(entityType, splashPerTick, amount, 2, 0.5f, 0.1f, 0.3f, soundEvent);
    }

    public static <T> FluidShootingBehavior<T> ofSplash(EntityType<? extends SplashEntity<T>> entityType, int splashPerTick, long amount, float baseSpeed, float extraSpeed,
                                                        float initialDivergence, float maxDivergence, Holder<SoundEvent> soundEvent) {
        return new ShootProjectileEntity<>((world, fluid, a) -> {
            var splash = entityType.create(world, EntitySpawnReason.SPAWN_ITEM_USE);
            assert splash != null;
            splash.setFluidData(fluid.data());
            return splash;
        }, splashPerTick, (w, a,b, c) -> amount, baseSpeed, extraSpeed, initialDivergence, maxDivergence, soundEvent);
    }

    public static <T> FluidShootingBehavior<T> ofEntity(EntityType<?> entityType, int splashPerTick, long amount, float baseSpeed, float extraSpeed,
                                                    float initialDivergence, float maxDivergence, Holder<SoundEvent> soundEvent) {
        return new ShootProjectileEntity<>((world, fluid, a) -> entityType.create(world, EntitySpawnReason.SPAWN_ITEM_USE), splashPerTick, (w, a, b, c) -> amount, baseSpeed, extraSpeed, initialDivergence, maxDivergence, soundEvent);
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
        divergence += context.extraSpread();
        var world = context.world();
        for (int i = 0; i < this.splashPerTick; i++) {
            var entity = entityCreator.createEntity(world, fluidInstance, amount);
            if (entity instanceof Projectile projectile) {
                ((ProjectileAccessor) projectile).setOwner(EntityReference.of(context.uuid()));

                if (!context.isEntity()) {
                    ((ProjectileAccessor) projectile).setLeftOwner(true);
                }
            }
            entity.setPos(pos);
            vec.set(rotation.x, rotation.y, rotation.z);
            vec.add((float) random.triangle(0, divergence),
                    (float) random.triangle(0, divergence),
                    (float) random.triangle(0, divergence));
            vec.normalize();
            vec.mul((this.baseSpeed + random.nextFloat() * this.extraSpeed) * context.force());

            entity.setDeltaMovement(vec.x, vec.y, vec.z);
            world.addFreshEntity(entity);
        }
        world.playSound(null, pos.x, pos.y, pos.z, this.soundEvent, context.soundCategory(), 0.5f, (float) random.triangle(1, 0.1));
    }

    @Override
    public void stopShooting(ShooterContext context, FluidInstance<T> fluidInstance) {}


    public interface AmountGetter<T> {
        long getAmount(ServerLevel world, FluidContainer container, FluidInstance<T> instance, boolean isCheck);
    }

    public interface EntityCreator<T> {
        Entity createEntity(ServerLevel world, FluidInstance<T> fluid, long amount);
    }
}