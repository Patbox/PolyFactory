package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.util.math.Direction;

public interface FluidInput {
    long insertFluid(FluidInstance<?> type, long amount, Direction direction);

    interface ContainerBased extends FluidInput {
        @Override
        default long insertFluid(FluidInstance<?> type, long amount, Direction direction) {
            return getFluidContainer(direction).insert(type, amount, false);
        }

        FluidContainer getFluidContainer(Direction direction);
    }
}
