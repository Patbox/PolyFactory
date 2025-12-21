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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public abstract class PipeLikeBlockEntity extends BlockEntity implements FluidInput.ContainerBased, DebugTextProvider {
    protected final FluidContainerImpl container = this.createContainer();
    protected final FluidWorldPushInteraction fluidPush;
    protected final FluidWorldPullInteraction fluidPull;

    public PipeLikeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.fluidPush = new FluidWorldPushInteraction(container, () -> (ServerLevel) level, this::getBlockPos);
        this.fluidPull = new FluidWorldPullInteraction(container, () -> (ServerLevel) level, this::getBlockPos);
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        this.container.writeData(view, "fluid");
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.container.readData(view, "fluid");
        this.fluidPush.setMaxPush(this.container.stored());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var f = components.get(FactoryDataComponents.FLUID);
        if (f != null) {
            f.copyTo(this.container);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
        componentMapBuilder.set(FactoryDataComponents.FLUID, FluidComponent.copyFrom(this.container));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        view.discard("fluid");
        super.removeComponentsFromTag(view);
    }

    public void preTick() {
        FluidContainerUtil.tick(this.container, (ServerLevel) level, worldPosition, this.container.fluidTemperature(), this::dropItem);
        this.fluidPush.lowerProgress(0.01);
        this.fluidPull.lowerProgress(0.01);
    }

    private void dropItem(ItemStack stack) {
        Containers.dropItemStack(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, stack);
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
        return FluidContainerImpl.singleFluid(FluidConstants.BLOCK, this::setChanged);
    }

    @Override
    public Component getDebugText() {
        return Component.literal("F: " + this.container.getFilledPercentage());
    }

    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.hasDirection(direction) ? this.container : null;
    }

    protected abstract boolean hasDirection(Direction direction);
}
