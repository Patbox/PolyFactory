package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FunnelBlockEntity extends BlockEntity {
    public FunnelBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FUNNEL, pos, state);
    }

    private ItemStack filterStack = ItemStack.EMPTY;

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("FilterStack", this.filterStack.writeNbt(new NbtCompound()));
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        this.updateHologram();
        return super.toInitialChunkDataNbt();
    }

    private void updateHologram() {
        var type = BlockBoundAttachment.get(this.world, this.pos);

        if (type != null && type.holder() instanceof FunnelBlock.Model model) {
            model.filterElement.setItem(this.filterStack);
            model.tick();
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.filterStack = ItemStack.fromNbt(nbt.getCompound("FilterStack"));
    }

    public boolean matches(ItemStack stack) {
        return filterStack.isEmpty() || ItemStack.canCombine(this.filterStack, stack);
    }

    public void setFilter(ItemStack stack) {
        this.filterStack = stack.copy();
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
}
