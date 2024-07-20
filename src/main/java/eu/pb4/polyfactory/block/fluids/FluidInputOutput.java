package eu.pb4.polyfactory.block.fluids;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface FluidInputOutput extends FluidOutput, FluidInput {
    interface ContainerBased extends FluidOutput.ContainerBased, FluidInput.ContainerBased {

    }

    interface Getter extends FluidOutput.Getter, FluidInput.Getter {
        FluidInputOutput getFluidInputOutput(ServerWorld world, BlockPos pos, Direction direction);

        @Override
        default FluidInput getFluidInput(ServerWorld world, BlockPos pos, Direction direction) {
            return getFluidInputOutput(world, pos, direction);
        }

        @Override
        default FluidOutput getFluidOutput(ServerWorld world, BlockPos pos, Direction direction) {
            return getFluidInputOutput(world, pos, direction);
        }
    }
}
