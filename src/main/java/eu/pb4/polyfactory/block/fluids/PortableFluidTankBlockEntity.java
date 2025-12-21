package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.smeltery.FaucedBlock;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class PortableFluidTankBlockEntity extends BlockEntity implements FluidInputOutput.ContainerBased, BlockEntityExtraListener {
    public static final long CAPACITY = FluidConstants.BLOCK * 4;
    private final FluidContainerImpl container = new FluidContainerImpl(CAPACITY, this::onFluidChanged);
    private float blockTemperature = 0;
    @Nullable
    private PortableFluidTankBlock.Model model;

    private boolean faucedActivate = false;
    private FaucedBlock.FaucedProvider faucedProvider = FaucedBlock.FaucedProvider.EMPTY;
    @Nullable
    private FluidInstance<?> faucedFluid = null;
    private float faucedRate = 0;

    public PortableFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PORTABLE_FLUID_TANK, pos, state);
    }

    public FluidContainer getFluidContainer() {
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
        if (this.model != null) {
            this.model.setFluid(this.container);
        }
    }
    @Override
    public void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var f = components.get(FactoryDataComponents.FLUID);
        if (f != null) {
            this.container.clear();
            f.extractTo(this.container);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
        componentMapBuilder.set(FactoryDataComponents.FLUID, FluidComponent.copyFrom(this.container));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard("fluid");
    }
    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.getBlockState().getValue(PortableFluidTankBlock.FACING) == direction ? this.container : null;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    private void onFluidChanged() {
        this.setChanged();
        if (this.model != null) {
            this.model.setFluid(this.container);
        }
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PortableFluidTankBlockEntity self) || (!(world instanceof ServerLevel serverWorld))) {
            return;
        }

        if (self.faucedProvider != FaucedBlock.FaucedProvider.EMPTY &&
                (!self.faucedProvider.isValid() && self.faucedActivate || self.container.isFull() || self.faucedProvider.getFluidContainerInput().get(self.faucedFluid) == 0)) {
            self.faucedProvider.setActiveFluid(null);
            self.faucedProvider = FaucedBlock.FaucedProvider.EMPTY;
            self.faucedActivate = false;
        }

        if (self.faucedActivate && self.faucedFluid != null) {
            var maxFlow = self.faucedFluid.getMaxFlow(serverWorld);
            var amount = Math.min(
                    Math.min(
                            Math.min((long) (self.faucedRate * 0.05 * maxFlow * self.faucedFluid.getFlowSpeedMultiplier(serverWorld)), maxFlow),
                            self.container.empty()),
                    self.faucedProvider.getFluidContainerInput().get(self.faucedFluid));
            self.faucedProvider.setActiveFluid(self.faucedFluid);
            self.faucedProvider.extract(self.faucedFluid, amount);
            self.container.insert(self.faucedFluid, amount, false);

            ((ServerLevel) world).sendParticles(self.faucedFluid.particle(),
                    pos.getX() + 0.5 + self.faucedProvider.direction().getStepX() / 32f,
                    pos.getY() + 1 + 4 / 16f,
                    pos.getZ() + 0.5 + self.faucedProvider.direction().getStepZ() / 32f,
                    0,
                    0, -1, 0, 0.1);
            return;
        }

        self.blockTemperature = BlockHeat.getReceived(world, pos) + self.container.fluidTemperature();
        FluidContainerUtil.tick(self.container, (ServerLevel) world, pos, self.blockTemperature, self::dropItem);
    }

    private void dropItem(ItemStack stack) {
        Containers.dropItemStack(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, stack);
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, worldPosition);
        if (x != null && x.holder() instanceof PortableFluidTankBlock.Model model) {
            this.model = model;
            this.model.setFluid(this.container);
        }
    }

    public InteractionResult activate(FaucedBlock.FaucedProvider provider, float rate) {
        if (!provider.isValid() || this.container.isFull() || provider.getFluidContainerInput().isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (this.faucedActivate && provider == this.faucedProvider) {
            this.faucedActivate = false;
            this.faucedProvider.setActiveFluid(null);
            this.faucedProvider = FaucedBlock.FaucedProvider.EMPTY;
            return InteractionResult.SUCCESS_SERVER;
        }

        this.faucedProvider = provider;
        this.faucedActivate = true;
        this.faucedRate = rate;
        this.faucedFluid = provider.getFluidContainerInput().fluids().getFirst();

        for (var fluid : this.container.fluids()) {
            if (provider.getFluidContainerInput().get(fluid) > 0) {
                this.faucedFluid = fluid;
                break;
            }
        }

        return InteractionResult.SUCCESS_SERVER;
    }
}
