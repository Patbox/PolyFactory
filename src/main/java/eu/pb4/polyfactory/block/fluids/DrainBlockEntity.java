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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        this.container.writeData(view, "fluid");
        if (!this.catalyst.isEmpty()) {
            view.store("catalyst", ItemStack.OPTIONAL_CODEC, this.catalyst);
        }
        updateModel();
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.container.readData(view, "fluid");
        this.setCatalyst(view.read("catalyst", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
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
        return this.container;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    private void onFluidChanged() {
        this.setChanged();
        this.updateModel();
    }

    private void updateModel() {
        if (this.model != null) {
            this.model.setFluid(this.container.topFluid(), this.container.getFilledPercentage());
            this.model.setCatalyst(this.catalyst);
        }
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, worldPosition);
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
        this.setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null) {
            Containers.dropContents(level, pos, NonNullList.of(ItemStack.EMPTY, this.catalyst));
        }
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof DrainBlockEntity be)) {
            return;
        }

        if (world.isRainingAt(pos.above())) {
            // Actually done some math:
            // For 49 cauldrons, it took 4 days to get 18 lvl-1, 15 lvl-2 and 4 lvl-3.
            // So 60 bottles total. 15 bottles per day, 15×27000 = 405000 droplets per day
            // 405000÷24000 = 16,875 droplets per tick. Then halved it and rounded down to just 8 droplets per tick (as it felt too fast).
            // Writing this, so I can remember why I choose this value.
            be.container.insert(FactoryFluids.WATER.defaultInstance(), 8, false);
        }
    }
}
