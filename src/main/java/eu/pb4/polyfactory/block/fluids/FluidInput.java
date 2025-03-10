package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface FluidInput {
    long insertFluid(FluidInstance<?> type, long amount, Direction direction);

    interface ContainerBased extends FluidInput, FluidContainerOwner {
        @Override
        default long insertFluid(FluidInstance<?> type, long amount, Direction direction) {
            var x = getFluidContainer(direction);

            return x != null ? x.insert(type, amount, false) : amount;
        }
    }

    interface Getter {
        FluidInput getFluidInput(ServerWorld world, BlockPos pos, Direction direction);
    }
}
