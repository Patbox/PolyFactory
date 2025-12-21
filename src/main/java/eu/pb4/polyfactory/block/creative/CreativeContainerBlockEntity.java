package eu.pb4.polyfactory.block.creative;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.other.ContainerBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("UnstableApiUsage")
public class CreativeContainerBlockEntity extends ContainerBlockEntity {
    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.CREATIVE_CONTAINER);
    }

    public CreativeContainerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CREATIVE_CONTAINER, pos, state);
    }

    @Override
    protected SingleItemStorage createStorage() {
        return new SingleItemStorage() {
            @Override
            protected long getCapacity(ItemVariant variant) {
                return variant.getItem().getDefaultMaxStackSize();
            }

            @Override
            public long extract(ItemVariant extractedVariant, long maxAmount, TransactionContext transaction) {
                return Math.min(this.amount, maxAmount);
            }

            @Override
            public long insert(ItemVariant insertedVariant, long maxAmount, TransactionContext transaction) {
                return Math.min(variant.getItem().getDefaultMaxStackSize() - this.amount, maxAmount);
            }

            @Override
            protected void readSnapshot(ResourceAmount<ItemVariant> snapshot) {
            }

            @Override
            protected void onFinalCommit() {
            }
        };
    }

    @Override
    public ItemStack extract(int amount) {
        return this.getItemStack().copyWithCount((int) Math.min(amount, this.storage.amount));
    }

    @Override
    public void setItemStack(ItemStack stack) {
        this.storage.amount = stack.getCount();
        super.setItemStack(stack);
    }

}
