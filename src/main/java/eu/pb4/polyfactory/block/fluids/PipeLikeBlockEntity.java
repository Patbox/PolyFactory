package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.util.DebugTextProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PipeLikeBlockEntity extends BlockEntity implements FluidInput.ContainerBased, DebugTextProvider {
    public static final long CAPACITY = FluidConstants.BLOCK;
    protected final FluidContainer container = FluidContainer.singleFluid(CAPACITY, this::markDirty);
    protected final BlockPos.Mutable mut = new BlockPos.Mutable();
    protected final double[] pullOverflow = new double[Direction.values().length];
    protected BlockState[] pullState = new BlockState[pullOverflow.length];
    protected final double[] pushOverflow = new double[Direction.values().length];
    protected BlockState[] pushState = new BlockState[pushOverflow.length];

    protected long maxPush = 0;
    public PipeLikeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidContainer getFluidContainer() {
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

    public void preTick() {
        this.container.tick((ServerWorld) world, pos, 0, this::dropItem);
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
            }
            return;
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
            var fluid = fluidOutput.getTopFluid(direction.getOpposite());
            if (fluid == null || (fluid.equals(currentFluid))) {
                return;
            }
            var maxFlow = fluid.getMaxFlow((ServerWorld) world);
            var amount = Math.min(Math.min((long) (strength * maxFlow * fluid.getFlowSpeedMultiplier((ServerWorld) world)),
                    maxFlow), this.container.empty());

            var extracted = fluidOutput.extractFluid(fluid, amount, direction.getOpposite());
            this.container.insert(fluid, extracted, false);
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

    @Override
    public Text getDebugText() {
        return Text.literal("F: " + this.container.getFilledPercentage());
    }

    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.container;
    }
}
