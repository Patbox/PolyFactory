package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Collection;
import java.util.List;

public interface FluidOutput {
    long extractFluid(FluidInstance<?> type, long amount, Direction direction, boolean change);
    Collection<FluidInstance<?>> getContainedFluids(Direction direction);

    interface ContainerBased extends FluidOutput, FluidContainerOwner {
        @Override
        default long extractFluid(FluidInstance<?> type, long amount, Direction direction, boolean change) {
            var x = getFluidContainer(direction);

            return x != null ? (change ? x.extract(type, amount, false) : Math.min(amount, x.get(type))) : 0;
        }

        @Override
        default Collection<FluidInstance<?>> getContainedFluids(Direction direction) {
            var x = getFluidContainer(direction);
            return x != null ? x.fluids() : List.of();
        }
    }

    interface Getter {
        FluidOutput getFluidOutput(ServerWorld world, BlockPos pos, Direction direction);
    }
}
