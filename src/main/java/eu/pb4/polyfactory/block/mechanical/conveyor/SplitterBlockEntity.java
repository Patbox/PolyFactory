package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.item.tool.FilterItem;
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

import java.util.function.Predicate;

public class SplitterBlockEntity extends BlockEntity {
    private FilterItem.Data filterLeft = FilterItem.createData(ItemStack.EMPTY, false);
    private FilterItem.Data filterRight = FilterItem.createData(ItemStack.EMPTY, false);
    private ItemStack filterStackLeft = ItemStack.EMPTY;
    private ItemStack filterStackRight = ItemStack.EMPTY;

    public SplitterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.SPLITTER, pos, state);
    }

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
            model.updateFilters(filterLeft.icon(), filterRight.icon());
            model.tick();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.filterStackLeft = ItemStack.fromNbt(nbt.getCompound("FilterStackLeft"));
        this.filterStackRight = ItemStack.fromNbt(nbt.getCompound("FilterStackRight"));
        this.filterRight = FilterItem.createData(this.filterStackRight, false);
        this.filterLeft = FilterItem.createData(this.filterStackLeft, false);
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
        this.filterLeft = FilterItem.createData(stack, false);
        this.markDirty();
        this.updateHologram();
    }

    public ItemStack getFilterRight() {
        return this.filterStackRight;
    }

    public void setFilterRight(ItemStack stack) {
        this.filterStackRight = stack;
        this.filterRight = FilterItem.createData(stack, false);

        this.markDirty();
        this.updateHologram();
    }
}
