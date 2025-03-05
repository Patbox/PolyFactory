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
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class FunnelBlockEntity extends LockableBlockEntity implements BlockEntityExtraListener {
    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.FUNNEL);
    }

    private FunnelBlock.Model model;
    private ItemStack filterStack = ItemStack.EMPTY;
    private FilterData filter = FilterData.of(ItemStack.EMPTY, true);
    private final Storage<ItemVariant> storage = new FilteredRedirectedStorage<>(ItemStorage.SIDED,
            this::getWorld, this::getPos, () -> this.getCachedState().get(FunnelBlock.FACING), (t) -> this.matches(t.toStack()));

    public FunnelBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FUNNEL, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.put("FilterStack", this.filterStack.encodeAllowEmpty(lookup));
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
    }


    private void updateHologram() {
        if (this.model != null) {
            model.filterElement.setFilter(this.filter);
            model.tick();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        this.filterStack = ItemStack.fromNbtOrEmpty(lookup, nbt.getCompound("FilterStack"));
        this.filter = FilterData.of(this.filterStack, true);
    }

    public boolean matches(ItemStack stack) {
        return this.filter.test(stack);
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    public ItemStack getFilter() {
        return this.filterStack;
    }

    public void setFilter(ItemStack stack) {
        this.filterStack = stack;
        this.filter = FilterData.of(stack, true);
        this.markDirty();
        this.updateHologram();
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.pos).holder() instanceof FunnelBlock.Model model ? model : null;
        this.updateHologram();
    }
}
