package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface FluidOutput {
    long extractFluid(FluidInstance<?> type, long amount, Direction direction);
    @Nullable
    FluidInstance<?> getTopFluid(Direction direction);

    interface ContainerBased extends FluidOutput, FluidContainerOwner {
        @Override
        default long extractFluid(FluidInstance<?> type, long amount, Direction direction) {
            var x = getFluidContainer(direction);

            return x != null ? x.extract(type, amount, false) : 0;
        }

        @Override
        @Nullable
        default FluidInstance<?> getTopFluid(Direction direction) {
            var x = getFluidContainer(direction);

            return x != null ? x.topFluid() : null;
        }
    }

    interface Getter {
        FluidOutput getFluidOutput(ServerWorld world, BlockPos pos, Direction direction);
    }
}
