package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

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
        FluidInput getFluidInput(ServerLevel world, BlockPos pos, Direction direction);
    }
}
