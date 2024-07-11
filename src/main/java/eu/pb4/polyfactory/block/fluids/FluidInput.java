package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidType;
import net.minecraft.util.math.Direction;

public interface FluidInput {
    long insertFluid(FluidType type, long amount, Direction direction);

    interface ContainerBased extends FluidInput {
        @Override
        default long insertFluid(FluidType type, long amount, Direction direction) {
            return getFluidContainer().insert(type, amount, false);
        }

        FluidContainer getFluidContainer();
    }
}
