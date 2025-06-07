package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.PredicateLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polyfactory.util.storage.WrappingStorage;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class ItemPackerBlockEntity extends LockableBlockEntity implements BlockEntityExtraListener, FilledStateProvider, SingleStackInventory {
    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> {
            var facing = self.getCachedState().get(ItemPackerBlock.FACING);

            if (dir != null && facing.getAxis() == dir.getAxis()) {
                return self.inventoryStorage;
            }

            return self.getItemStorage();
        }, FactoryBlockEntities.ITEM_PACKER);
    }

    private ItemStack itemStack = ItemStack.EMPTY;
    private ItemPackerBlock.Model model;
    private final InventoryStorage inventoryStorage = InventoryStorage.of(this, null);
    @Nullable
    private Storage<ItemVariant> cachedItemStorage;

    public ItemPackerBlockEntity(BlockPos pos, BlockState state) {
        this(FactoryBlockEntities.ITEM_PACKER, pos, state);
    }

    protected ItemPackerBlockEntity(BlockEntityType<? extends ItemPackerBlockEntity> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (!this.itemStack.isEmpty()) {
            view.put("item", ItemStack.OPTIONAL_CODEC, this.itemStack);
        }
    }

    @Override
    public void readData(ReadView view) {
        super.readData(view);
        setStack(view.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public ItemStack getStack() {
        return this.itemStack;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.itemStack = stack;
        this.cachedItemStorage = null;
        this.markDirty();
        if (this.model != null) {
            this.model.setDisplay(this.itemStack);
        }
    }


    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.pos).holder() instanceof ItemPackerBlock.Model model ? model : null;
        if (this.model != null) {
            this.model.setDisplay(this.itemStack);
        }
    }

    @Override
    public @Nullable Text getFilledStateText() {
        if (this.itemStack.isEmpty()) {
            return ScreenTexts.EMPTY;
        }

        return Text.translatable("text.polyfactory.x_out_of_y", this.getFilledAmount(), this.getFillCapacity());
    }

    @Override
    public long getFilledAmount() {
        if (this.itemStack.getItem() instanceof BundleItem bundleItem) {
            var oc = this.itemStack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT).getOccupancy();
            return oc.getNumerator() * 64L / oc.getDenominator();
        }

        var x = getItemStorage();
        if (x == null) {
            return 0;
        }

        long out = 0;
        for (var view : x) {
            out += view.getAmount();
        }

        return out;
    }

    @Override
    public long getFillCapacity() {
        if (this.itemStack.getItem() instanceof BundleItem bundleItem) {
            //var oc = this.itemStack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT).getOccupancy();
            return 64;
        }

        var x = getItemStorage();
        if (x == null) {
            return 0;
        }

        long out = 0;
        for (var view : x) {
            out += view.getCapacity();
        }

        return out;
    }

    @Override
    public int getMaxCount(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (dir != null && this.getCachedState().get(ItemPackerBlock.FACING).getAxis() == dir.getAxis()) {
            return ItemStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
        }

        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return dir != null && this.getCachedState().get(ItemPackerBlock.FACING).getAxis() == dir.getAxis();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return this.getCachedState().get(ItemPackerBlock.FACING).getAxis() == side.getAxis() ? SingleStackInventory.super.getAvailableSlots(side) : new int[0];
    }

    private Storage<ItemVariant> getItemStorage() {
        if (this.cachedItemStorage != null) {
            return this.cachedItemStorage;
        }

        var storage = ItemStorage.ITEM.find(this.itemStack, ContainerItemContext.ofSingleSlot(this.inventoryStorage.getSlot(0)));

        return WrappingStorage.withModifyCallback(storage, this::runAdvancement);
    }

    private void runAdvancement() {
        if (this.world != null && FactoryUtil.getClosestPlayer(this.world, this.pos, 16) instanceof ServerPlayerEntity player) {
            TriggerCriterion.trigger(player, FactoryTriggers.ITEM_PACKER_ACCESSES);
        }
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);
        if (this.world != null) {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.getStack());
        }
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity);
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(ItemPackerBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(2, new PredicateLimitedSlot(ItemPackerBlockEntity.this, 0, stack -> ItemStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null));
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (ItemPackerBlockEntity.this.isRemoved() || player.getPos().squaredDistanceTo(Vec3d.ofCenter(ItemPackerBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
