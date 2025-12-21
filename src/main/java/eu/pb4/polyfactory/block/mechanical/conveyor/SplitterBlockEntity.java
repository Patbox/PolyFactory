package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SplitterBlockEntity extends LockableBlockEntity implements BlockEntityExtraListener {
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
    protected void saveAdditional(ValueOutput view) {
        if (!this.filterStackLeft.isEmpty()) {
            view.store("FilterStackLeft", ItemStack.OPTIONAL_CODEC, this.filterStackLeft);
        }
        if (!this.filterStackRight.isEmpty()) {
            view.store("FilterStackRight", ItemStack.OPTIONAL_CODEC, this.filterStackRight);
        }
        view.putByte("CurPos", (byte) this.position);
    }

    private void updateHologram() {
        if (this.model != null) {
            model.updateFilters(filterLeft, filterRight);
            model.tick();
        }
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.filterStackLeft = view.read("FilterStackLeft", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.filterStackRight = view.read("FilterStackRight", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.filterRight = FilterData.of(this.filterStackRight, false);
        this.filterLeft = FilterData.of(this.filterStackLeft, false);
        this.filtersEqual = Objects.equals(this.filterLeft.filter(), this.filterRight.filter());
        this.position = view.getByteOr("CurPos", (byte) 0);
    }

    public int pos(int max) {
        var c = this.position + 1;
        if (c >= max) {
            c = 0;
        }
        this.position = c;
        this.setChanged();
        return c;
    }

    public boolean matchesLeft(ItemStack stack) {
        return this.filterLeft.test(stack);
    }

    public boolean matchesRight(ItemStack stack) {
        return this.filterRight.test(stack);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public ItemStack getFilterLeft() {
        return this.filterStackLeft;
    }

    public void setFilterLeft(ItemStack stack) {
        this.filterStackLeft = stack;
        this.filterLeft = FilterData.of(stack, false);
        this.filtersEqual = Objects.equals(this.filterLeft.filter(), this.filterRight.filter());
        this.setChanged();
        this.updateHologram();
    }

    public ItemStack getFilterRight() {
        return this.filterStackRight;
    }

    public void setFilterRight(ItemStack stack) {
        this.filterStackRight = stack;
        this.filterRight = FilterData.of(stack, false);
        this.filtersEqual = Objects.equals(this.filterLeft.filter(), this.filterRight.filter());
        this.setChanged();
        this.updateHologram();
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.worldPosition).holder() instanceof SplitterBlock.Model model ? model : null;
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

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.getFilterRight());
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.getFilterLeft());
        }
    }
}
