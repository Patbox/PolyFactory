package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface FluidOutput {
    long extractFluid(FluidInstance<?> type, long amount, Direction direction);
    FluidInstance<?> getTopFluid(Direction direction);

    interface ContainerBased extends FluidOutput {
        @Override
        default long extractFluid(FluidInstance<?> type, long amount, Direction direction) {
            return getFluidContainer(direction).extract(type, amount, false);
        }

        @Override
        default FluidInstance<?> getTopFluid(Direction direction) {
            return getFluidContainer(direction).topFluid();
        };

        FluidContainer getFluidContainer(Direction direction);
    }

    interface Getter {
        FluidOutput getFluidOutput(ServerWorld world, BlockPos pos, Direction direction);
    }
}
