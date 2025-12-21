package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public interface FluidOutput {
    long extractFluid(FluidInstance<?> type, long amount, Direction direction, boolean change);
    Collection<FluidInstance<?>> getContainedFluids(Direction direction);
    FluidContainerInput getFluidContainerInput(Direction direction);

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

        @Override
        default FluidContainerInput getFluidContainerInput(Direction direction) {
            var x = getFluidContainer(direction);
            return x != null ? FluidContainerInput.of(x) : FluidContainerInput.EMPTY;
        };
    }

    interface Getter {
        FluidOutput getFluidOutput(ServerLevel world, BlockPos pos, Direction direction);
    }
}
