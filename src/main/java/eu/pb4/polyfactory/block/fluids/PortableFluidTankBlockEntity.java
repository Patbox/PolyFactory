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
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
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
    protected void writeData(WriteView view) {
        super.writeData(view);
        this.container.writeData(view, "fluid");
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.container.readData(view, "fluid");
        if (this.model != null) {
            this.model.setFluid(this.container);
        }
    }
    @Override
    public void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        var f = components.get(FactoryDataComponents.FLUID);
        if (f != null) {
            this.container.clear();
            f.extractTo(this.container);
        }
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        componentMapBuilder.add(FactoryDataComponents.FLUID, FluidComponent.copyFrom(this.container));
    }

    @Override
    public void removeFromCopiedStackData(WriteView view) {
        super.removeFromCopiedStackData(view);
        view.remove("fluid");
    }
    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.getCachedState().get(PortableFluidTankBlock.FACING) == direction ? this.container : null;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    private void onFluidChanged() {
        this.markDirty();
        if (this.model != null) {
            this.model.setFluid(this.container);
        }
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PortableFluidTankBlockEntity self) || (!(world instanceof ServerWorld serverWorld))) {
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

            ((ServerWorld) world).spawnParticles(self.faucedFluid.particle(),
                    pos.getX() + 0.5 + self.faucedProvider.direction().getOffsetX() / 32f,
                    pos.getY() + 1 + 4 / 16f,
                    pos.getZ() + 0.5 + self.faucedProvider.direction().getOffsetZ() / 32f,
                    0,
                    0, -1, 0, 0.1);
            return;
        }

        self.blockTemperature = BlockHeat.getReceived(world, pos) + self.container.fluidTemperature();
        FluidContainerUtil.tick(self.container, (ServerWorld) world, pos, self.blockTemperature, self::dropItem);
    }

    private void dropItem(ItemStack stack) {
        ItemScatterer.spawn(world, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, stack);
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, pos);
        if (x != null && x.holder() instanceof PortableFluidTankBlock.Model model) {
            this.model = model;
            this.model.setFluid(this.container);
        }
    }

    public ActionResult activate(FaucedBlock.FaucedProvider provider, float rate) {
        if (!provider.isValid() || this.container.isFull() || provider.getFluidContainerInput().isEmpty()) {
            return ActionResult.FAIL;
        }
        if (this.faucedActivate && provider == this.faucedProvider) {
            this.faucedActivate = false;
            this.faucedProvider.setActiveFluid(null);
            this.faucedProvider = FaucedBlock.FaucedProvider.EMPTY;
            return ActionResult.SUCCESS_SERVER;
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

        return ActionResult.SUCCESS_SERVER;
    }
}
