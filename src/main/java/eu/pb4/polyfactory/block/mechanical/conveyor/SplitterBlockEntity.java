package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.polyfactory.block.base.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Objects;

public class SplitterBlockEntity extends BlockEntity implements BlockEntityExtraListener {
    private FilterData filterLeft = FilterData.EMPTY_FALSE;
    private FilterData filterRight = FilterData.EMPTY_FALSE;
    private ItemStack filterStackLeft = ItemStack.EMPTY;
    private ItemStack filterStackRight = ItemStack.EMPTY;
    private SplitterBlock.Model model;
    private boolean filtersEqual = true;

    private int position;

    public SplitterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.SPLITTER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("FilterStackLeft", this.filterStackLeft.writeNbt(new NbtCompound()));
        nbt.put("FilterStackRight", this.filterStackRight.writeNbt(new NbtCompound()));
        nbt.putByte("CurPos", (byte) this.position);
    }

    private void updateHologram() {
        if (this.model != null) {
            model.updateFilters(filterLeft.icon(), filterRight.icon());
            model.tick();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.filterStackLeft = ItemStack.fromNbt(nbt.getCompound("FilterStackLeft"));
        this.filterStackRight = ItemStack.fromNbt(nbt.getCompound("FilterStackRight"));
        this.filterRight = FilterData.of(this.filterStackRight, false);
        this.filterLeft = FilterData.of(this.filterStackLeft, false);
        this.filtersEqual = Objects.equals(this.filterLeft.filter(), this.filterRight.filter());
        this.position = nbt.getByte("CurPos");
    }

    public int pos(int max) {
        var c = this.position + 1;
        if (c >= max) {
            c = 0;
        }
        this.position = c;
        this.markDirty();
        return c;
    }

    public boolean matchesLeft(ItemStack stack) {
        return this.filterLeft.test(stack);
    }

    public boolean matchesRight(ItemStack stack) {
        return this.filterRight.test(stack);
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    public ItemStack getFilterLeft() {
        return this.filterStackLeft;
    }

    public void setFilterLeft(ItemStack stack) {
        this.filterStackLeft = stack;
        this.filterLeft = FilterData.of(stack, false);
        this.filtersEqual = Objects.equals(this.filterLeft.filter(), this.filterRight.filter());
        this.markDirty();
        this.updateHologram();
    }

    public ItemStack getFilterRight() {
        return this.filterStackRight;
    }

    public void setFilterRight(ItemStack stack) {
        this.filterStackRight = stack;
        this.filterRight = FilterData.of(stack, false);
        this.filtersEqual = Objects.equals(this.filterLeft.filter(), this.filterRight.filter());
        this.markDirty();
        this.updateHologram();
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.pos).holder() instanceof SplitterBlock.Model model ? model : null;
        this.updateHologram();
    }

    public boolean matchesSides(ItemStack stack) {
        return this.filtersEqual && this.filterLeft.test(stack);
    }

    public boolean filtersEmpty() {
        return this.filtersEqual && this.filterLeft.isEmpty();
    }

    public boolean isLeftFilterEmpty() {
        return this.filterLeft.isEmpty();
    }

    public boolean isRightFilterEmpty() {
        return this.filterRight.isEmpty();
    }
}
