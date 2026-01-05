package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polyfactory.util.storage.FilteredRedirectedStorage;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class FunnelBlockEntity extends LockableBlockEntity implements BlockEntityExtraListener, FunnelBlock.CommonBlockEntity {
    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.FUNNEL);
    }

    private FunnelBlock.Model model;
    private ItemStack filterStack = ItemStack.EMPTY;
    private FilterData filter = FilterData.of(ItemStack.EMPTY, true);

    private int maxStackSize = 64;

    private final Storage<ItemVariant> storage = new FilteredRedirectedStorage<>(ItemStorage.SIDED,
            this::getLevel, this::getBlockPos, () -> this.getBlockState().getValue(FunnelBlock.FACING), (t) -> this.matches(t.toStack()),
            (t) -> Math.min(t.getComponentMap().getOrDefault(DataComponents.MAX_STACK_SIZE, 1), this.maxStackSize));

    public FunnelBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FUNNEL, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        if (!this.filterStack.isEmpty()) {
            view.store("FilterStack", ItemStack.OPTIONAL_CODEC, this.filterStack);
        }
        view.putInt("max_stack_size", this.maxStackSize);

    }

    @Override
    public int maxStackSize() {
        return maxStackSize;
    }

    @Override
    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        this.updateHologram();
        this.setChanged();
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
    }


    private void updateHologram() {
        if (this.model != null) {
            model.filterElement.setFilter(this.filter);
            model.countElement.setText(Component.literal("[x" + this.maxStackSize + "]"));
            model.updateFacing(this.getBlockState());
            model.tick();
        }
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.filterStack = view.read("FilterStack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.filter = FilterData.of(this.filterStack, true);
        this.maxStackSize = view.getIntOr("max_stack_size", 64);
        this.updateHologram();
    }

    public boolean matches(ItemStack stack) {
        return this.filter.test(stack);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public ItemStack getFilter() {
        return this.filterStack;
    }

    public void setFilter(ItemStack stack) {
        this.filterStack = stack;
        this.filter = FilterData.of(stack, true);
        this.setChanged();
        this.updateHologram();
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.worldPosition).holder() instanceof FunnelBlock.Model model ? model : null;
        this.updateHologram();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.getFilter());
        }
    }
}
