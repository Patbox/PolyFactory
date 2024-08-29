package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.*;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.util.DebugTextProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public abstract class PipeLikeBlockEntity extends BlockEntity implements FluidInput.ContainerBased, DebugTextProvider {
    protected final FluidContainerImpl container = this.createContainer();
    protected final BlockPos.Mutable mut = new BlockPos.Mutable();
    protected final double[] pullOverflow = new double[Direction.values().length];
    protected BlockState[] pullState = new BlockState[pullOverflow.length];
    protected final double[] pushOverflow = new double[Direction.values().length];
    protected BlockState[] pushState = new BlockState[pushOverflow.length];
    protected long maxPush = 0;
    private final int[] particleCounter = {1, 3, 5, 6, 8, 4};

    public PipeLikeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("fluid", this.container.toNbt(registryLookup));
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.container.fromNbt(registryLookup, nbt, "fluid");
        this.maxPush = this.container.stored();
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        var f = components.get(FactoryDataComponents.FLUID);
        if (f != null) {
            f.copyTo(this.container);
        }
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        componentMapBuilder.add(FactoryDataComponents.FLUID, FluidComponent.copyFrom(this.container));
    }

    @Override
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        nbt.remove("fluid");
    }

    public void preTick() {
        FluidContainerUtil.tick(this.container, (ServerWorld) world, pos, this.container.fluidTemperature(), this::dropItem);
        for (int i = 0; i < this.pullOverflow.length; i++) {
            this.pullOverflow[i] = Math.max(0, this.pullOverflow[i] - 0.01);
            this.pushOverflow[i] = Math.max(0, this.pushOverflow[i] - 0.01);
        }
    }

    private void dropItem(ItemStack stack) {
        ItemScatterer.spawn(world, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, stack);
    }

    public void postTick() {
        this.maxPush = this.container.stored();
    }

    public void pushFluid(Direction direction, double strength) {
        mut.set(pos).move(direction);
        var pushedBlockState = world.getBlockState(mut);
        FluidInput fluidInput = null;
        if (pushedBlockState.getBlock() instanceof FluidInput.Getter getter) {
            fluidInput = getter.getFluidInput((ServerWorld) world, mut, direction.getOpposite());
        } else if (world.getBlockEntity(mut) instanceof FluidInput f) {
            fluidInput = f;
        }

        if (fluidInput != null) {
            var fluid = this.container.topFluid();
            if (fluid != null) {
                var maxFlow = fluid.getMaxFlow((ServerWorld) world);
                var amount = Math.min(Math.min((long) (strength * maxFlow * fluid.getFlowSpeedMultiplier((ServerWorld) world)),
                        maxFlow), this.maxPush);

                var extracted = this.container.extract(fluid, amount, false);
                var leftover = fluidInput.insertFluid(fluid, extracted, direction.getOpposite());
                if (leftover != 0) {
                    this.container.insert(fluid, leftover, false);
                }
                this.maxPush -= extracted - leftover;
                this.pushOverflow[direction.ordinal()] = 0;
            }
            return;
        }
        var fluid = this.container.topFluid();

        if (pushedBlockState.isAir() && fluid != null && fluid.type() == FactoryFluids.EXPERIENCE && this.container.get(fluid) >= FluidBehaviours.EXPERIENCE_ORB_TO_FLUID && world.random.nextBoolean()) {
            var max = (int) Math.min(this.container.get(fluid) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, 30);
            var amount = max <= 1 ? 1 : world.random.nextBetween(1, max);
            this.container.extract(fluid, amount * FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, false);
            var x = new ExperienceOrbEntity(world, this.pos.getX() + 0.5 + direction.getOffsetX() * 0.7,
                    this.pos.getY() + 0.5 + direction.getOffsetY() * 0.7,
                    this.pos.getZ() + 0.5 + direction.getOffsetZ() * 0.7, amount);
            world.spawnEntity(x);
        }

        if (pushedBlockState.isAir() && (this.particleCounter[direction.ordinal()]++ % 3) == 0) {
            if (fluid != null) {
                var particle = fluid.particle();
                if (particle != null) {
                    ((ServerWorld) world).spawnParticles(particle,
                            this.pos.getX() + 0.5 + direction.getOffsetX() * 0.55,
                            this.pos.getY() + 0.5 + direction.getOffsetY() * 0.55,
                            this.pos.getZ() + 0.5 + direction.getOffsetZ() * 0.55,
                            0,
                            direction.getOffsetX() * 0.1, direction.getOffsetY() * 0.1, direction.getOffsetZ() * 0.1, 0.4
                    );
                }
            }
        }

        if (pushedBlockState == this.pushState[direction.ordinal()]) {
            var possibilities = FluidBehaviours.BLOCK_STATE_TO_FLUID_INSERT.get(pushedBlockState);
            if (possibilities != null) {
                for (var insert : possibilities) {
                    if (insert != null && this.container.canExtract(insert.getLeft(), true)) {
                        var maxFlow = insert.getLeft().instance().getMaxFlow((ServerWorld) world);
                        var amount = Math.min(Math.min((long) (strength * maxFlow * insert.getLeft().instance().getFlowSpeedMultiplier((ServerWorld) world)),
                                maxFlow), this.maxPush);
                        this.pushOverflow[direction.ordinal()] += amount;
                        if (this.pushOverflow[direction.ordinal()] >= insert.getLeft().amount()) {
                            world.setBlockState(mut, insert.getRight());
                            this.container.extract(insert.getLeft(), false);
                            this.pushOverflow[direction.ordinal()] = 0;
                            break;
                        }
                    }
                }
            }
        } else {
            this.pushState[direction.ordinal()] = pushedBlockState;
            this.pushOverflow[direction.ordinal()] = 0;
        }
    }

    public void pullFluid(Direction direction, double strength) {
        mut.set(pos).move(direction);
        var pulledBlockState = world.getBlockState(mut);

        var currentFluid = this.container.topFluid();
        FluidOutput fluidOutput = null;
        if (pulledBlockState.getBlock() instanceof FluidOutput.Getter getter) {
            fluidOutput = getter.getFluidOutput((ServerWorld) world, mut, direction.getOpposite());
        } else if (world.getBlockEntity(mut) instanceof FluidOutput f) {
            fluidOutput = f;
        }

        if (fluidOutput != null) {
            for (var fluid : fluidOutput.getContainedFluids(direction.getOpposite())) {
                if (fluid == null || (currentFluid != null && !currentFluid.equals(fluid))) {
                    continue;
                }
                var maxFlow = fluid.getMaxFlow((ServerWorld) world);
                var amount = Math.min(Math.min((long) (strength * maxFlow * fluid.getFlowSpeedMultiplier((ServerWorld) world)),
                        maxFlow), this.container.empty());
                var extracted = fluidOutput.extractFluid(fluid, amount, direction.getOpposite(), false);
                var leftover = this.container.insert(fluid, extracted, false);
                if (extracted != leftover) {
                    fluidOutput.extractFluid(fluid, extracted - leftover, direction.getOpposite(), true);
                    return;
                }
            }

            return;
        }

        if (pulledBlockState == this.pullState[direction.ordinal()]) {
            var extract = FluidBehaviours.BLOCK_STATE_TO_FLUID_EXTRACT.get(pulledBlockState);
            if (extract != null && this.container.canInsert(extract.getLeft(), true)) {
                var maxFlow = extract.getLeft().instance().getMaxFlow((ServerWorld) world);
                var amount = Math.min(Math.min((long) (strength * maxFlow * extract.getLeft().instance().getFlowSpeedMultiplier((ServerWorld) world)),
                        maxFlow), this.container.empty());
                this.pullOverflow[direction.ordinal()] += amount;
                if (this.pullOverflow[direction.ordinal()] >= extract.getLeft().amount()) {
                    world.setBlockState(mut, extract.getRight());
                    this.container.insert(extract.getLeft(), false);
                    this.pullOverflow[direction.ordinal()] = 0;
                }
            }
        } else {
            this.pullState[direction.ordinal()] = pulledBlockState;
            this.pullOverflow[direction.ordinal()] = 0;
        }
    }

    protected FluidContainerImpl createContainer() {
        return FluidContainerImpl.singleFluid(FluidConstants.BLOCK, this::markDirty);
    }

    @Override
    public Text getDebugText() {
        return Text.literal("F: " + this.container.getFilledPercentage());
    }

    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.hasDirection(direction) ? this.container : null;
    }

    protected abstract boolean hasDirection(Direction direction);
}
