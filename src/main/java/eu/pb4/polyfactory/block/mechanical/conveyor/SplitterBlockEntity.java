package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SplitterBlockEntity extends BlockEntity {
    public SplitterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.SPLITTER, pos, state);
    }

    private ItemStack filterStackLeft = ItemStack.EMPTY;
    private ItemStack filterStackRight = ItemStack.EMPTY;

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("FilterStackLeft", this.filterStackLeft.writeNbt(new NbtCompound()));
        nbt.put("FilterStackRight", this.filterStackRight.writeNbt(new NbtCompound()));
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        this.updateHologram();
        return super.toInitialChunkDataNbt();
    }

    private void updateHologram() {
        var type = BlockBoundAttachment.get(this.world, this.pos);

        if (type != null && type.holder() instanceof SplitterBlock.Model model) {
            model.updateFilters(filterStackLeft, filterStackRight);
            model.tick();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.filterStackLeft = ItemStack.fromNbt(nbt.getCompound("FilterStackLeft"));
        this.filterStackRight = ItemStack.fromNbt(nbt.getCompound("FilterStackLeft"));
    }

    public boolean matchesLeft(ItemStack stack) {
        return !filterStackLeft.isEmpty() && ItemStack.canCombine(this.filterStackLeft, stack);
    }

    public boolean matchesRight(ItemStack stack) {
        return !filterStackRight.isEmpty() && ItemStack.canCombine(this.filterStackRight, stack);
    }

    public void setFilterLeft(ItemStack stack) {
        this.filterStackLeft = stack.copy();
        this.markDirty();
        this.updateHologram();
    }

    public void setFilterRight(ItemStack stack) {
        this.filterStackRight = stack.copy();
        this.markDirty();
        this.updateHologram();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    public ItemStack getFilterLeft() {
        return this.filterStackLeft;
    }

    public ItemStack getFilterRight() {
        return this.filterStackRight;
    }
}
