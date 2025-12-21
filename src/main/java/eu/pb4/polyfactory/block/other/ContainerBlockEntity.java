package eu.pb4.polyfactory.block.other;

import com.mojang.serialization.Dynamic;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.mixin.TagValueInputAccessor;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ContainerBlockEntity extends LockableBlockEntity implements BlockEntityExtraListener, FilledStateProvider {
    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.CONTAINER);
    }

    private ItemStack itemStack = ItemStack.EMPTY;
    private ContainerBlock.Model model;

    public ContainerBlockEntity(BlockPos pos, BlockState state) {
        this(FactoryBlockEntities.CONTAINER, pos, state);
    }

    protected ContainerBlockEntity(BlockEntityType<? extends ContainerBlockEntity> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }    public final SingleItemStorage storage = createStorage();

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
                ContainerBlockEntity.this.setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, ContainerBlockEntity.this.getBlockState().getBlock());
                updateStackWithTick();
            }
        };
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        this.storage.writeData(view);
        // amount -> long
        // variant -> { item -> string/id, tag -> compound/null }
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        updateStackWithTick();
        super.loadAdditional(view);
        if (view instanceof TagValueInput) {
            var nbt = ((TagValueInputAccessor) view).getInput();
            try {
                if (nbt.getCompoundOrEmpty("variant").contains("tag")) {
                    var hack = new CompoundTag();
                    var variant = nbt.getCompoundOrEmpty("variant");
                    hack.put("tag", variant.get("tag"));
                    hack.put("id", variant.get("item"));
                    hack.putInt("Count", 1);
                    var updated = (CompoundTag) DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, hack), 3700, SharedConstants.WORLD_VERSION).getValue();
                    variant.put("components", updated.get("components"));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        this.storage.readData(view);
    }

    private void updateStack() {
        this.itemStack = this.storage.variant.toStack();
        if (this.model != null) {
            model.setDisplay(this.itemStack, this.storage.amount);
        }
    }

    private void updateStackWithTick() {
        updateStack();
        if (this.model != null && this.level != null) {
            model.tick();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null) {
            var count = this.storage.amount;
            var max = this.getItemStack().getMaxStackSize();
            while (count > 0) {
                var stack = this.storage.variant.toStack((int) Math.min(max, count));
                count -= stack.getCount();
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
    }

    public int getMaxCount(ItemStack stack) {
        return getMaxStackCount() * stack.getMaxStackSize();
    }

    protected int getMaxStackCount() {
        return this.getBlockState().getBlock() instanceof ContainerBlock c ? c.maxStackCount : 0;
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
            this.setChanged();
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
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.worldPosition).holder() instanceof ContainerBlock.Model model ? model : null;
        this.updateStackWithTick();
    }

    @Override
    public @Nullable Component getFilledStateText() {
        return Component.translatable("text.polyfactory.x_out_of_y", this.storage.amount, this.storage.getCapacity());
    }

    @Override
    public long getFilledAmount() {
        return this.storage.getAmount();
    }

    @Override
    public long getFillCapacity() {
        return this.storage.getCapacity();
    }


}
