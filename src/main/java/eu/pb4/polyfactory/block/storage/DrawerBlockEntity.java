package eu.pb4.polyfactory.block.storage;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.stream.IntStream;

// Fixme - Implement better inventory emulation or use FAPI's item transaction thingy api. Currently breaks if something tries to increase last slot
public class DrawerBlockEntity extends BlockEntity implements SidedInventory {
    public DrawerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.DRAWER, pos, state);
    }
    private static final int MAX_COUNT_MULT = 9 * 3;
    private static final int[] SLOTS = IntStream.range(0, MAX_COUNT_MULT).toArray();

    private ItemStack itemStack = ItemStack.EMPTY;
    private int count = 0;

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("ItemStack", this.itemStack.writeNbt(new NbtCompound()));
        nbt.putInt("ItemCount", this.count);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        this.updateHologram();
        return super.toInitialChunkDataNbt();
    }

    private void updateHologram() {
        var type = BlockBoundAttachment.get(this.world, this.pos);

        if (type != null && type.holder() instanceof DrawerBlock.Model model) {
            model.setDisplay(this.itemStack, this.count);
            model.tick();
        }
    }

    public static int getMaxCount(ItemStack stack) {
        return MAX_COUNT_MULT * stack.getMaxCount();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.itemStack = ItemStack.fromNbt(nbt.getCompound("FilterStackLeft"));
        this.count = nbt.getInt("ItemCount");
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return (this.itemStack.isEmpty() || ItemStack.canCombine(this.itemStack, stack)) && this.count + stack.getCount() <= getMaxCount(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public int size() {
        return MAX_COUNT_MULT;
    }

    @Override
    public boolean isEmpty() {
        return this.count <= 0;
    }

    @Override
    public ItemStack getStack(int slot) {
        var s = this.count / 64;

        if (s < slot) {
            return this.itemStack.copyWithCount(64);
        } else if (slot + 1 == MAX_COUNT_MULT) {
            return this.itemStack.copyWithCount(this.count % 64);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        var x = Math.min(this.count, amount);
        this.count -= x;
        this.updateHologram();
        return this.itemStack.copyWithCount(x);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return removeStack(slot, this.itemStack.getMaxCount());
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (this.count == 0) {
            if (!stack.isEmpty()) {
                this.itemStack = stack.copyWithCount(1);
            } else {
                this.itemStack = ItemStack.EMPTY;
                this.updateHologram();
                return;
            }
        }

        var s = this.count / 64;

        if (s < slot) {
            this.count -= (64 - stack.getCount());
        } else if (slot + 1 == MAX_COUNT_MULT) {
            var i = this.count % 64;
            this.count -= (i - stack.getCount());
        } else {
            this.count += stack.getCount();
        }

        this.updateHologram();
    }

    @Override
    public boolean containsAny(Predicate<ItemStack> predicate) {
        return predicate.test(this.itemStack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        this.count = 0;
    }

    public boolean matches(ItemStack stackInHand) {
        return ItemStack.canCombine(this.itemStack, stackInHand);
    }

    public int addItems(int i) {
        var out = this.count + i;
        var max = getMaxCount(this.itemStack);
        this.count = Math.min(out, max);
        this.updateHologram();
        return Math.max(out - max, 0);
    }
}
