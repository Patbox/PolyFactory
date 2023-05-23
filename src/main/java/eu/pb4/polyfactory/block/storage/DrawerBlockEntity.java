package eu.pb4.polyfactory.block.storage;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("UnstableApiUsage")
public class DrawerBlockEntity extends BlockEntity {
    public DrawerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.DRAWER, pos, state);
    }
    private static final int MAX_COUNT_MULT = 9 * 4;
    public final SingleItemStorage storage = new SingleItemStorage() {
        @Override
        protected long getCapacity(ItemVariant variant) {
            return DrawerBlockEntity.getMaxCount(DrawerBlockEntity.this.itemStack);
        }

        @Override
        public long extract(ItemVariant extractedVariant, long maxAmount, TransactionContext transaction) {
            var v = this.variant;
            var i = super.extract(extractedVariant, maxAmount, transaction);
            this.variant = v;
            return i;
        }

        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            DrawerBlockEntity.this.markDirty();
            updateHologram();
        }
    };

    private ItemStack itemStack = ItemStack.EMPTY;

    @Override
    protected void writeNbt(NbtCompound nbt) {
        this.storage.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.storage.readNbt(nbt);
        this.itemStack = this.storage.variant.toStack();
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        this.updateHologram();
        return super.toInitialChunkDataNbt();
    }

    private void updateHologram() {
        var type = BlockBoundAttachment.get(this.world, this.pos);
        if (this.itemStack == ItemStack.EMPTY) {
            this.itemStack = this.storage.variant.toStack();
        }

        if (type != null && type.holder() instanceof DrawerBlock.Model model) {
            model.setDisplay(this.itemStack, this.storage.amount);
            model.tick();
        }
    }

    public static int getMaxCount(ItemStack stack) {
        return 9 * 4 * stack.getMaxCount();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    public boolean matches(ItemStack stackInHand) {
        return ItemStack.canCombine(this.itemStack, stackInHand);
    }

    public void setItemStack(ItemStack stack) {
        this.itemStack = stack.copyWithCount(1);
        this.storage.variant = ItemVariant.of(stack);
        this.updateHologram();
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public ItemStack extract(int amount) {
        try (var t = Transaction.openOuter()) {
            var i = this.storage.extract(this.storage.variant, amount, t);
            t.commit();
            return this.itemStack.copyWithCount((int) i);
        }
    }


    public ItemStack extractStack() {
        return extract(this.itemStack.getMaxCount());
    }

    public int addItems(int i) {
        try (var t = Transaction.openOuter()) {
            i = (int) this.storage.insert(this.storage.variant, i, t);
            t.commit();
            return i;
        }
    }

    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.DRAWER);
    }

    public boolean isEmpty() {
        return this.storage.amount == 0;
    }
}
