package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidContainer;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface FluidContainerOwner {
    @Nullable
    FluidContainer getFluidContainer(Direction direction);
}
