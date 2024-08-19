package eu.pb4.polyfactory.fluid.shooting;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;

public interface FluidShootingBehavior<T> {
        boolean canShoot(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container);
        void startShooting(ShooterContext context, FluidInstance<T> fluidInstance, FluidContainer container);
        void continueShooting(ShooterContext context, FluidInstance<T> fluidInstance, int tick, FluidContainer container);
        void stopShooting(ShooterContext context, FluidInstance<T> fluidInstance);
    }