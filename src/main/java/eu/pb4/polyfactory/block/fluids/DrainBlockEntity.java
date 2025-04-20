package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class DrainBlockEntity extends BlockEntity implements FluidInputOutput.ContainerBased, BlockEntityExtraListener {
    public static final long CAPACITY = FluidConstants.BLOCK;
    private final FluidContainerImpl container = new FluidContainerImpl(CAPACITY, this::onFluidChanged);

    private ItemStack catalyst = ItemStack.EMPTY;
    @Nullable
    private DrainBlock.Model model;

    public DrainBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.DRAIN, pos, state);
    }
    public DrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("fluid", this.container.toNbt(registryLookup));
        if (!this.catalyst.isEmpty()) {
            nbt.put("catalyst", this.catalyst.toNbt(registryLookup));
        }
        updateModel();
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.container.fromNbt(registryLookup, nbt, "fluid");
        this.setCatalyst(FactoryUtil.fromNbtStack(registryLookup, nbt.getCompoundOrEmpty("catalyst")));
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
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
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        nbt.remove("fluid");
    }

    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.container;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    private void onFluidChanged() {
        this.markDirty();
        this.updateModel();
    }

    private void updateModel() {
        if (this.model != null) {
            this.model.setFluid(this.container.topFluid(), this.container.getFilledPercentage());
            this.model.setCatalyst(this.catalyst);
        }
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, pos);
        if (x != null && x.holder() instanceof DrainBlock.Model model) {
            this.model = model;
            this.updateModel();
        }
    }

    public ItemStack catalyst() {
        return catalyst;
    }

    public void setCatalyst(ItemStack catalyst) {
        this.catalyst = catalyst;
        if (this.model != null) {
            this.model.setCatalyst(this.catalyst);
        }
        this.markDirty();
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);
        if (this.world != null) {
            ItemScatterer.spawn(world, pos, DefaultedList.copyOf(ItemStack.EMPTY, this.catalyst));
        }
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof DrainBlockEntity be)) {
            return;
        }

        if (world.hasRain(pos.up())) {
            // Actually done some math:
            // For 49 cauldrons, it took 4 days to get 18 lvl-1, 15 lvl-2 and 4 lvl-3.
            // So 60 bottles total. 15 bottles per day, 15×27000 = 405000 droplets per day
            // 405000÷24000 = 16,875 droplets per tick. Then halved it and rounded down to just 8 droplets per tick (as it felt too fast).
            // Writing this, so I can remember why I choose this value.
            be.container.insert(FactoryFluids.WATER.defaultInstance(), 8, false);
        }
    }
}
