package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidType;
import net.minecraft.util.math.Direction;

public interface FluidOutput {
    long extractFluid(FluidType type, long amount, Direction direction);

    interface ContainerBased extends FluidOutput {
        @Override
        default long extractFluid(FluidType type, long amount, Direction direction) {
            return getFluidContainer().extract(type, amount, false);
        }

        FluidContainer getFluidContainer();
    }
}
