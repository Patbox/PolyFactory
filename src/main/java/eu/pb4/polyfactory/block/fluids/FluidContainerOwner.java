package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidContainer;
import net.minecraft.util.math.Direction;

public interface FluidContainerOwner {
    FluidContainer getFluidContainer(Direction direction);
}
