package eu.pb4.polyfactory.fluid.world;

import eu.pb4.polyfactory.block.fluids.FluidInput;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Supplier;

public final class FluidWorldPushInteraction {
    private final double[] pushOverflow = new double[Direction.values().length];
    private final int[] particleCounter = {1, 3, 5, 6, 8, 4};
    private final FluidContainer container;
    private final Supplier<ServerLevel> world;
    private final Supplier<BlockPos> pos;
    private final BlockState[] pushState = new BlockState[pushOverflow.length];
    private long maxPush = 0;

    public FluidWorldPushInteraction(FluidContainer container, Supplier<ServerLevel> world, Supplier<BlockPos> pos) {
        this.container = container;
        this.world = world;
        this.pos = pos;
    }


    public void pushFluid(Direction direction, double strength) {
        var pos = this.pos();
        var world = this.world();
        var container = this.container();
        var mut = this.pos().mutable().move(direction);
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
            var amount = max <= 1 ? 1 : world.random.nextIntBetweenInclusive(1, max);
            container.extract(fluid, amount * FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, false);
            var x = new ExperienceOrb(world, pos.getX() + 0.5 + direction.getStepX() * 0.7,
                    pos.getY() + 0.5 + direction.getStepY() * 0.7,
                    pos.getZ() + 0.5 + direction.getStepZ() * 0.7, amount);
            world.addFreshEntity(x);
        }

        if (pushedBlockState.isAir() && (this.particleCounter(direction) % 3) == 0) {
            if (fluid != null) {
                var particle = fluid.particle();
                if (particle != null) {
                    world.sendParticles(particle,
                            pos.getX() + 0.5 + direction.getStepX() * 0.55,
                            pos.getY() + 0.5 + direction.getStepY() * 0.55,
                            pos.getZ() + 0.5 + direction.getStepZ() * 0.55,
                            0,
                            direction.getStepX() * 0.1, direction.getStepY() * 0.1, direction.getStepZ() * 0.1, 0.4
                    );
                }
            }
        }

        if (pushedBlockState == this.getPushState(direction)) {
            var possibilities = FluidBehaviours.BLOCK_STATE_TO_FLUID_INSERT.get(pushedBlockState);
            if (possibilities != null) {
                for (var insert : possibilities) {
                    if (insert != null && container.canExtract(insert.getA(), true)) {
                        var maxFlow = insert.getA().instance().getMaxFlow(world);
                        var amount = Math.min(Math.min((long) (strength * maxFlow * insert.getA().instance().getFlowSpeedMultiplier(world)),
                                maxFlow), this.maxPush());
                        this.setPushOverflow(direction, this.getPushOverflow(direction) + amount);
                        if (this.getPushOverflow(direction) >= insert.getA().amount()) {
                            world.setBlockAndUpdate(mut, insert.getB());
                            container.extract(insert.getA(), false);
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

    public ServerLevel world() {
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
