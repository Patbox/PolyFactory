package eu.pb4.polyfactory.block.other;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.datafixer.Schemas;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ContainerBlockEntity extends LockableBlockEntity implements BlockEntityExtraListener, FilledStateProvider {
    static  {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.CONTAINER);
    }

    private ItemStack itemStack = ItemStack.EMPTY;
    private ContainerBlock.Model model;

    public ContainerBlockEntity(BlockPos pos, BlockState state) {
        this(FactoryBlockEntities.CONTAINER, pos, state);
    }
    public final SingleItemStorage storage = createStorage();

    protected ContainerBlockEntity(BlockEntityType<? extends ContainerBlockEntity> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    protected SingleItemStorage createStorage() {
        return new SingleItemStorage() {
            @Override
            protected long getCapacity(ItemVariant variant) {
                return ContainerBlockEntity.this.getMaxCount(ContainerBlockEntity.this.itemStack);
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
                ContainerBlockEntity.this.markDirty();
                world.updateComparators(pos, ContainerBlockEntity.this.getCachedState().getBlock());
                updateStackWithTick();
            }
        };
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        this.storage.writeNbt(nbt, lookup);
        // amount -> long
        // variant -> { item -> string/id, tag -> compound/null }
        super.writeNbt(nbt, lookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        try {
            if (nbt.getCompound("variant").contains("tag")) {
                var hack = new NbtCompound();
                var variant = nbt.getCompound("variant");
                hack.put("tag", variant.get("tag"));
                hack.put("id", variant.get("item"));
                hack.putInt("Count", 1);
                var updated = (NbtCompound) Schemas.getFixer().update(TypeReferences.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, hack), 3700, SharedConstants.getGameVersion().getSaveVersion().getId()).getValue();
                variant.put("components", updated.get("components"));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }


        this.storage.readNbt(nbt, lookup);

        updateStackWithTick();
        super.readNbt(nbt, lookup);
    }

    private void updateStack() {
        this.itemStack = this.storage.variant.toStack();
        if (this.model != null) {
            model.setDisplay(this.itemStack, this.storage.amount);
        }
    }

    private void updateStackWithTick() {
        updateStack();
        if (this.model != null && this.world != null) {
            model.tick();
        }
    }

    public int getMaxCount(ItemStack stack) {
        return getMaxStackCount() * stack.getMaxCount();
    }

    protected int getMaxStackCount() {
        return this.getCachedState().getBlock() instanceof ContainerBlock c ? c.maxStackCount : 0;
    }

    public boolean matches(ItemStack stackInHand) {
        return this.storage.variant.matches(stackInHand);
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setItemStack(ItemStack stack) {
        this.storage.variant = ItemVariant.of(stack);
        this.updateStackWithTick();
    }

    public ItemStack extract(int amount) {
        try (var t = Transaction.openOuter()) {
            var variant = this.storage.variant;
            var i = this.storage.extract(variant, amount, t);
            t.commit();
            this.markDirty();
            return variant.toStack((int) i);
        }
    }

    public int addItems(int i) {
        try (var t = Transaction.openOuter()) {
            i = (int) this.storage.insert(this.storage.variant, i, t);
            t.commit();
            return i;
        }
    }

    public boolean isEmpty() {
        return this.storage.amount == 0;
    }


    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.pos).holder() instanceof ContainerBlock.Model model ? model : null;
        this.updateStackWithTick();
    }

    @Override
    public @Nullable Text getFilledStateText() {
        return Text.translatable("text.polyfactory.x_out_of_y", this.storage.amount, this.storage.getCapacity());
    }
}
