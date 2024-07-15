package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.util.DebugTextProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PipeLikeBlockEntity extends BlockEntity implements FluidInput.ContainerBased, DebugTextProvider {
    public static final long CAPACITY = FluidConstants.BLOCK;
    protected final FluidContainer container = FluidContainer.singleFluid(CAPACITY, this::markDirty);
    protected final BlockPos.Mutable mut = new BlockPos.Mutable();
    protected final double[] pullOverflow = new double[Direction.values().length];
    protected BlockState pullState = Blocks.AIR.getDefaultState();
    protected final double[] pushOverflow = new double[Direction.values().length];
    protected BlockState pushState = Blocks.AIR.getDefaultState();

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
        for (int i = 0; i < this.pullOverflow.length; i++) {
            this.pullOverflow[i] = Math.max(0, this.pullOverflow[i] - 0.02);
            this.pushOverflow[i] = Math.max(0, this.pushOverflow[i] - 0.02);
        }
    }

    public void postTick() {
        this.maxPush = this.container.stored();
    }

    public void pushFluid(Direction direction, double strength) {
        var amount = Math.min(Math.min((long) (strength * FluidConstants.BOTTLE), FluidConstants.BOTTLE), this.maxPush);
        var be = world.getBlockEntity(mut.set(pos).move(direction));
        if (be instanceof FluidInput fluidInput) {
            var fluid = this.container.topFluid();
            var extracted = this.container.extract(fluid, amount, false);
            var leftover = fluidInput.insertFluid(fluid, extracted, direction.getOpposite());
            if (leftover != 0) {
                this.container.insert(fluid, leftover, false);
            }
            this.maxPush -= extracted - leftover;
            return;
        }
        var pushedBlockState = world.getBlockState(mut);

        if (pushedBlockState == this.pushState) {
            this.pushOverflow[direction.ordinal()] += amount;
            var possibilities = FluidBehaviours.BLOCK_STATE_TO_FLUID_INSERT.get(pushedBlockState);
            if (possibilities != null) {
                for (var insert : possibilities) {
                    if (insert != null && this.pushOverflow[direction.ordinal()] >= insert.getLeft().amount() && this.container.canExtract(insert.getLeft(), true)) {
                        world.setBlockState(mut, insert.getRight());
                        this.container.extract(insert.getLeft(), false);
                        this.pushOverflow[direction.ordinal()] = 0;
                        break;
                    }
                }
            }
        } else {
            this.pushState = pushedBlockState;
            this.pushOverflow[direction.ordinal()] = 0;
        }
    }

    public void pullFluid(Direction direction, double strength) {
        var amount = Math.min(Math.min((long) (strength * FluidConstants.BOTTLE), FluidConstants.BOTTLE), this.container.empty());
        var be = world.getBlockEntity(mut.set(pos).move(direction));
        var currentFluid = this.container.topFluid();

        if (be instanceof FluidOutput fluidOutput) {
            var fluid = fluidOutput.getTopFluid(direction.getOpposite());
            if (fluid == null || (fluid.equals(currentFluid))) {
                return;
            }
            var extracted = fluidOutput.extractFluid(fluid, amount, direction.getOpposite());
            this.container.insert(fluid, extracted, false);
            return;
        }
        var pulledBlockState = world.getBlockState(mut);

        if (pulledBlockState == this.pullState) {
            this.pullOverflow[direction.ordinal()] += amount;
            var extract = FluidBehaviours.BLOCK_STATE_TO_FLUID_EXTRACT.get(pulledBlockState);
            if (extract != null && this.pullOverflow[direction.ordinal()] >= extract.getLeft().amount() && this.container.canInsert(extract.getLeft(), true)) {
                world.setBlockState(mut, extract.getRight());
                this.container.insert(extract.getLeft(), false);
                this.pullOverflow[direction.ordinal()] = 0;
            }
        } else {
            this.pullState = pulledBlockState;
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
