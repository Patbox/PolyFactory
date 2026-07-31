package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidExchangeHandler;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

public interface FluidOutput {
    long extractFluid(FluidInstance<?> type, long amount, Direction direction, boolean change);
    long getAmount(FluidInstance<?> type, Direction direction);
    List<FluidInstance<?>> getContainedFluids(Direction direction);
    default long getStoredFluidsTotal(Direction direction) {
        long x = 0;
        for (var fluid : getContainedFluids(direction)) {
            x += getAmount(fluid, direction);
        }
        return x;
    }

    default FluidExchangeHandler getFluidExchangeHandler(Direction direction) {
        return new FluidExchangeHandler() {
            @Override
            public boolean canInsert(FluidInstance<?> type, long amount, boolean exact) {
                return false;
            }

            @Override
            public long insert(FluidInstance<?> type, long amount, boolean exact) {
                return 0;
            }

            @Override
            public boolean canExtract(FluidInstance<?> type, long amount, boolean exact) {
                var value = extractFluid(type, amount, direction, false);
                return exact ? value == amount : value != 0;
            }

            @Override
            public long extract(FluidInstance<?> type, long amount, boolean exact) {
                if (canExtract(type, amount, exact)) {
                    return extractFluid(type, amount, direction, true);
                }
                return 0;
            }

            @Override
            public long get(FluidInstance<?> type) {
                return getAmount(type, direction);
            }

            @Override
            public List<FluidInstance<?>> fluids() {
                return getContainedFluids(direction);
            }

            @Override
            public long stored() {
                return getStoredFluidsTotal(direction);
            }

            @Override
            public long capacity() {
                return Long.MAX_VALUE;
            }
        };
    };

    interface ContainerBased extends FluidOutput, FluidContainerOwner {
        @Override
        default long extractFluid(FluidInstance<?> type, long amount, Direction direction, boolean change) {
            var x = getFluidContainer(direction);

            return x != null ? (change ? x.extract(type, amount, false) : Math.min(amount, x.get(type))) : 0;
        }

        @Override
        default long getAmount(FluidInstance<?> type, Direction direction) {
            var x = getFluidContainer(direction);

            return x != null ? x.get(type) : 0;
        }

        @Override
        default List<FluidInstance<?>> getContainedFluids(Direction direction) {
            var x = getFluidContainer(direction);
            return x != null ? x.fluids() : List.of();
        }

        @Override
        default long getStoredFluidsTotal(Direction direction) {
            var x = getFluidContainer(direction);
            return x != null ? x.stored() : 0;
        }
    }

    interface Getter {
        FluidOutput getFluidOutput(ServerLevel world, BlockPos pos, Direction direction);
    }
}
