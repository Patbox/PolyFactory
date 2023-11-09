package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polyfactory.util.inventory.FilteredRedirectedItemStorage;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class FunnelBlockEntity extends BlockEntity implements BlockEntityExtraListener {
    @SuppressWarnings("UnstableApiUsage")
    private final Storage<ItemVariant> storage = new FilteredRedirectedItemStorage<>(ItemStorage.SIDED,
            this::getWorld, this::getPos, () -> this.getCachedState().get(FunnelBlock.FACING), (t) -> this.matches(t.toStack()));
    static  {
        //noinspection UnstableApiUsage
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.FUNNEL);
    }
    private FunnelBlock.Model model;

    public FunnelBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FUNNEL, pos, state);
    }

    private ItemStack filterStack = ItemStack.EMPTY;
    private FilterData filter = FilterData.of(ItemStack.EMPTY, true);

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("FilterStack", this.filterStack.writeNbt(new NbtCompound()));
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
    }


    private void updateHologram() {

        if (this.model != null) {
            model.filterElement.setItem(this.filter.icon());
            model.tick();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.filterStack = ItemStack.fromNbt(nbt.getCompound("FilterStack"));
        this.filter = FilterData.of(this.filterStack, true);
    }

    public boolean matches(ItemStack stack) {
        return this.filter.test(stack);
    }

    public void setFilter(ItemStack stack) {
        this.filterStack = stack;
        this.filter =  FilterData.of(stack, true);
        this.markDirty();
        this.updateHologram();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    public ItemStack getFilter() {
        return this.filterStack;
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.pos).holder() instanceof FunnelBlock.Model model ? model : null;
        this.updateHologram();
    }
}
