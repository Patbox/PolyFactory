package eu.pb4.polyfactory.block.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public interface FluidInputOutput extends FluidOutput, FluidInput {
    interface ContainerBased extends FluidInputOutput, FluidOutput.ContainerBased, FluidInput.ContainerBased {

    }

    interface Getter extends FluidOutput.Getter, FluidInput.Getter {
        FluidInputOutput getFluidInputOutput(ServerLevel world, BlockPos pos, Direction direction);

        @Override
        default FluidInput getFluidInput(ServerLevel world, BlockPos pos, Direction direction) {
            return getFluidInputOutput(world, pos, direction);
        }

        @Override
        default FluidOutput getFluidOutput(ServerLevel world, BlockPos pos, Direction direction) {
            return getFluidInputOutput(world, pos, direction);
        }
    }
}
