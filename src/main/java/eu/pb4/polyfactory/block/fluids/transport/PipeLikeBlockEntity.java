package eu.pb4.polyfactory.block.fluids.transport;

import eu.pb4.polyfactory.block.fluids.FluidInput;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.world.FluidWorldPullInteraction;
import eu.pb4.polyfactory.fluid.world.FluidWorldPushInteraction;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.util.DebugTextProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public abstract class PipeLikeBlockEntity extends BlockEntity implements FluidInput.ContainerBased, DebugTextProvider {
    protected final FluidContainerImpl container = this.createContainer();
    protected final FluidWorldPushInteraction fluidPush;
    protected final FluidWorldPullInteraction fluidPull;

    public PipeLikeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.fluidPush = new FluidWorldPushInteraction(container, () -> (ServerWorld) world, this::getPos);
        this.fluidPull = new FluidWorldPullInteraction(container, () -> (ServerWorld) world, this::getPos);
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        this.container.writeData(view, "fluid");
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.container.readData(view, "fluid");
        this.fluidPush.setMaxPush(this.container.stored());
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
    public void removeFromCopiedStackData(WriteView view) {
        view.remove("fluid");
        super.removeFromCopiedStackData(view);
    }

    public void preTick() {
        FluidContainerUtil.tick(this.container, (ServerWorld) world, pos, this.container.fluidTemperature(), this::dropItem);
        this.fluidPush.lowerProgress(0.01);
        this.fluidPull.lowerProgress(0.01);
    }

    private void dropItem(ItemStack stack) {
        ItemScatterer.spawn(world, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, stack);
    }

    public void postTick() {
        this.fluidPush.setMaxPush(this.container.stored());
    }

    public final void pushFluid(Direction direction, double strength) {
        this.fluidPush.pushFluid(direction, strength);
    }

    public final void pullFluid(Direction direction, double strength) {
        this.fluidPull.pullFluid(direction, strength);
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
