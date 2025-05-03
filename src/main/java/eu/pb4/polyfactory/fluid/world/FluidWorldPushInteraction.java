package eu.pb4.polyfactory.fluid.world;

import eu.pb4.polyfactory.block.fluids.FluidInput;
import eu.pb4.polyfactory.block.fluids.FluidOutput;
import eu.pb4.polyfactory.block.fluids.PipeBaseBlock;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.*;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.function.Supplier;

public final class FluidWorldPushInteraction {
    private final double[] pushOverflow = new double[Direction.values().length];
    private final int[] particleCounter = {1, 3, 5, 6, 8, 4};
    private final FluidContainer container;
    private final Supplier<ServerWorld> world;
    private final Supplier<BlockPos> pos;
    private final BlockState[] pushState = new BlockState[pushOverflow.length];
    private long maxPush = 0;

    public FluidWorldPushInteraction(FluidContainer container, Supplier<ServerWorld> world, Supplier<BlockPos> pos) {
        this.container = container;
        this.world = world;
        this.pos = pos;
    }


    public void pushFluid(Direction direction, double strength) {
        var pos = this.pos();
        var world = this.world();
        var container = this.container();
        var mut = this.pos().mutableCopy().move(direction);
        var pushedBlockState = world.getBlockState(mut);
        FluidInput fluidInput = null;
        if (pushedBlockState.getBlock() instanceof FluidInput.Getter getter) {
            fluidInput = getter.getFluidInput(this.world(), mut, direction.getOpposite());
        } else if (world.getBlockEntity(mut) instanceof FluidInput f) {
            fluidInput = f;
        }

        if (fluidInput != null) {
            var fluid = this.container().topFluid();
            if (fluid != null) {
                var maxFlow = fluid.getMaxFlow(this.world());
                var amount = Math.min(Math.min((long) (strength * maxFlow * fluid.getFlowSpeedMultiplier(world)),
                        maxFlow), this.maxPush());

                var extracted = container.extract(fluid, amount, false);
                var leftover = fluidInput.insertFluid(fluid, extracted, direction.getOpposite());
                if (leftover != 0) {
                    container.insert(fluid, leftover, false);
                }
                this.pushed(extracted - leftover);
                this.setPushOverflow(direction, 0);
            }
            return;
        }
        var fluid = container.topFluid();

        if (pushedBlockState.isAir() && fluid != null && fluid.type() == FactoryFluids.EXPERIENCE && container.get(fluid) >= FluidBehaviours.EXPERIENCE_ORB_TO_FLUID && world.random.nextBoolean()) {
            var max = (int) Math.min(container.get(fluid) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, 30);
            var amount = max <= 1 ? 1 : world.random.nextBetween(1, max);
            container.extract(fluid, amount * FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, false);
            var x = new ExperienceOrbEntity(world, pos.getX() + 0.5 + direction.getOffsetX() * 0.7,
                    pos.getY() + 0.5 + direction.getOffsetY() * 0.7,
                    pos.getZ() + 0.5 + direction.getOffsetZ() * 0.7, amount);
            world.spawnEntity(x);
        }

        if (pushedBlockState.isAir() && (this.particleCounter(direction) % 3) == 0) {
            if (fluid != null) {
                var particle = fluid.particle();
                if (particle != null) {
                    world.spawnParticles(particle,
                            pos.getX() + 0.5 + direction.getOffsetX() * 0.55,
                            pos.getY() + 0.5 + direction.getOffsetY() * 0.55,
                            pos.getZ() + 0.5 + direction.getOffsetZ() * 0.55,
                            0,
                            direction.getOffsetX() * 0.1, direction.getOffsetY() * 0.1, direction.getOffsetZ() * 0.1, 0.4
                    );
                }
            }
        }

        if (pushedBlockState == this.getPushState(direction)) {
            var possibilities = FluidBehaviours.BLOCK_STATE_TO_FLUID_INSERT.get(pushedBlockState);
            if (possibilities != null) {
                for (var insert : possibilities) {
                    if (insert != null && container.canExtract(insert.getLeft(), true)) {
                        var maxFlow = insert.getLeft().instance().getMaxFlow(world);
                        var amount = Math.min(Math.min((long) (strength * maxFlow * insert.getLeft().instance().getFlowSpeedMultiplier(world)),
                                maxFlow), this.maxPush());
                        this.setPushOverflow(direction, this.getPushOverflow(direction) + amount);
                        if (this.getPushOverflow(direction) >= insert.getLeft().amount()) {
                            world.setBlockState(mut, insert.getRight());
                            container.extract(insert.getLeft(), false);
                            this.setPushOverflow(direction, 0);
                            break;
                        }
                    }
                }
            }
        } else {
            this.setPushState(direction, pushedBlockState);
            this.setPushOverflow(direction, 0);
        }
    }

    public long maxPush() {
        return this.maxPush;
    }

    public void setMaxPush(long maxPush) {
        this.maxPush = maxPush;
    }

    public long pushed(long amount) {
        return this.maxPush -= amount;
    }

    public double getPushOverflow(Direction dir) {
        return this.pushOverflow[dir.ordinal()];
    }

    public void setPushOverflow(Direction dir, double value) {
        this.pushOverflow[dir.ordinal()] = value;
    }

    public BlockState getPushState(Direction direction) {
        return this.pushState[direction.ordinal()];
    }

    public void setPushState(Direction direction, BlockState state) {
        this.pushState[direction.ordinal()] = state;
    }

    public int particleCounter(Direction direction) {
        return this.particleCounter[direction.ordinal()]++;
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
        for (int i = 0; i < this.pushOverflow.length; i++) {
            this.pushOverflow[i] = Math.max(0, this.pushOverflow[i] - amount);
        }
    }

}
