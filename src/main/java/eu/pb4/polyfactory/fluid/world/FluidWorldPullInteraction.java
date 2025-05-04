package eu.pb4.polyfactory.fluid.world;

import eu.pb4.polyfactory.block.fluids.FluidOutput;
import eu.pb4.polyfactory.block.fluids.transport.PipeBaseBlock;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.function.Supplier;

public final class FluidWorldPullInteraction {
    private final double[] pullOverflow = new double[Direction.values().length];
    private final BlockState[] pullState = new BlockState[pullOverflow.length];

    private final FluidContainer container;
    private final Supplier<ServerWorld> world;
    private final Supplier<BlockPos> pos;
    public FluidWorldPullInteraction(FluidContainer container, Supplier<ServerWorld> world, Supplier<BlockPos> pos) {
        this.container = container;
        this.world = world;
        this.pos = pos;
    }

    public void pullFluid(Direction direction, double strength) {
        var pos = this.pos();
        var world = this.world();
        var container = this.container();

        var mut = pos.mutableCopy().move(direction);
        var pulledBlockState = world.getBlockState(mut);

        var currentFluid = container.topFluid();
        FluidOutput fluidOutput = null;
        if (pulledBlockState.getBlock() instanceof FluidOutput.Getter getter) {
            fluidOutput = getter.getFluidOutput(world, mut, direction.getOpposite());
        } else if (world.getBlockEntity(mut) instanceof FluidOutput f) {
            fluidOutput = f;
        }

        if (fluidOutput != null) {
            for (var fluid : fluidOutput.getContainedFluids(direction.getOpposite())) {
                if (fluid == null || (currentFluid != null && !currentFluid.equals(fluid))) {
                    continue;
                }
                var maxFlow = fluid.getMaxFlow(world);
                var amount = Math.min(Math.min((long) (strength * maxFlow * fluid.getFlowSpeedMultiplier(world)),
                        maxFlow), container.empty());
                var extracted = fluidOutput.extractFluid(fluid, amount, direction.getOpposite(), false);
                var leftover = container.insert(fluid, extracted, false);
                if (extracted != leftover) {
                    fluidOutput.extractFluid(fluid, extracted - leftover, direction.getOpposite(), true);
                    return;
                }
            }

            return;
        }

        if (pulledBlockState == this.getPullState(direction)) {
            if (!pulledBlockState.getFluidState().isEmpty() && !(pulledBlockState.getBlock() instanceof PipeBaseBlock)) {
                Fluid stillFluid = null;
                Fluid flowingFluid = null;
                FluidInstance<?> fluidType = null;

                if (pulledBlockState.getFluidState().isIn(FluidTags.WATER)) {
                    fluidType = FactoryFluids.WATER.defaultInstance();
                    stillFluid = Fluids.WATER;
                    flowingFluid = Fluids.FLOWING_WATER;
                } else if (pulledBlockState.getFluidState().isIn(FluidTags.LAVA)) {
                    fluidType = FactoryFluids.LAVA.defaultInstance();
                    stillFluid = Fluids.LAVA;
                    flowingFluid = Fluids.FLOWING_LAVA;
                }

                if (fluidType != null) {
                    var maxFlow = fluidType.getMaxFlow(world);
                    var amount = Math.min(Math.min((long) (strength * maxFlow * fluidType.getFlowSpeedMultiplier(world)),
                            maxFlow), container.empty());
                    this.setPullOverflow(direction, this.getPullOverflow(direction) + amount);

                    if (this.getPullOverflow(direction) >= FluidConstants.BLOCK) {
                        var mut2 = mut.mutableCopy();
                        BlockState currentState = pulledBlockState;
                        while (true) {
                            if (currentState.getFluidState().isOf(stillFluid) || currentState.getFluidState().isOf(flowingFluid)) {
                                mut2.set(mut);
                                var nextState = world.getBlockState(mut2.move(Direction.UP));

                                if (nextState.getFluidState().isOf(stillFluid) || nextState.getFluidState().isOf(flowingFluid)) {
                                    mut.set(mut2);
                                    currentState = nextState;
                                    continue;
                                } else if (currentState.getFluidState().isOf(flowingFluid)) {
                                    int level = currentState.getFluidState().getLevel();
                                    boolean found = false;
                                    mut2.set(mut);
                                    for (var testDir : Direction.Type.HORIZONTAL) {
                                        nextState = world.getBlockState(mut2.move(testDir));
                                        if ((nextState.getFluidState().isOf(stillFluid) || nextState.getFluidState().isOf(flowingFluid)) && level < nextState.getFluidState().getLevel()) {
                                            mut.set(mut2);
                                            level = nextState.getFluidState().getLevel();
                                            currentState = nextState;
                                            found = true;
                                        }
                                        mut2.move(testDir, -1);
                                    }

                                    if (found) {
                                        continue;
                                    }
                                }
                            }
                            break;
                        }

                        if (currentState.getFluidState().isStill()) {
                            var insert = false;
                            if (currentState.getBlock() instanceof Waterloggable && fluidType.type() == FactoryFluids.WATER) {
                                world.setBlockState(mut, currentState.with(Properties.WATERLOGGED, false));
                                insert = true;
                            } else if (currentState.getBlock() instanceof FluidBlock) {
                                world.setBlockState(mut, Blocks.AIR.getDefaultState());
                                insert = true;
                            } else if (currentState.isReplaceable() || currentState.getBlock() instanceof AbstractPlantPartBlock) {
                                world.breakBlock(mut, true);
                                world.setBlockState(mut, Blocks.AIR.getDefaultState());
                                insert = true;
                            }
                            if (insert) {
                                container.insert(fluidType, FluidConstants.BLOCK, false);
                            }
                        }
                        this.setPullOverflow(direction, 0);
                    }
                    return;
                }
            }

            var extract = FluidBehaviours.BLOCK_STATE_TO_FLUID_EXTRACT.get(pulledBlockState);
            if (extract != null && container.canInsert(extract.getLeft(), true)) {
                var maxFlow = extract.getLeft().instance().getMaxFlow(world);
                var amount = Math.min(Math.min((long) (strength * maxFlow * extract.getLeft().instance().getFlowSpeedMultiplier(world)),
                        maxFlow), container.empty());
                this.setPullOverflow(direction, this.getPullOverflow(direction) + amount);
                if (this.getPullOverflow(direction) >= extract.getLeft().amount()) {
                    world.setBlockState(mut, extract.getRight());
                    container.insert(extract.getLeft(), false);
                    this.setPullOverflow(direction, 0);
                }
            }
        } else {
            this.setPullState(direction, pulledBlockState);
            this.setPullOverflow(direction, 0);
        }
    }

    public double getPullOverflow(Direction dir) {
        return this.pullOverflow[dir.ordinal()];
    }

    public void setPullOverflow(Direction dir, double value) {
        this.pullOverflow[dir.ordinal()] = value;

    }

    public BlockState getPullState(Direction direction) {
        return this.pullState[direction.ordinal()];
    }

    public void setPullState(Direction direction, BlockState state) {
        this.pullState[direction.ordinal()] = state;
    }

    public ServerWorld world() {
        return this.world.get();
    }

    public BlockPos pos() {
        return this.pos.get();
    }

    public FluidContainer container() {
        return this.container;
    }

    public void lowerProgress(double amount) {
        for (int i = 0; i < this.pullOverflow.length; i++) {
            this.pullOverflow[i] = Math.max(0, this.pullOverflow[i] - amount);
        }
    }
}
