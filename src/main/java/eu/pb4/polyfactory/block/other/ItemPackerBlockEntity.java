package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.PredicateLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SingleStackContainer;
import eu.pb4.polyfactory.util.storage.WrappingStorage;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ItemPackerBlockEntity extends LockableBlockEntity implements BlockEntityExtraListener, FilledStateProvider, SingleStackContainer {
    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> {
            var facing = self.getBlockState().getValue(ItemPackerBlock.FACING);

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
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (!this.itemStack.isEmpty()) {
            view.store("item", ItemStack.OPTIONAL_CODEC, this.itemStack);
        }
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
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
        this.setChanged();
        if (this.model != null) {
            this.model.setDisplay(this.itemStack);
        }
    }


    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.worldPosition).holder() instanceof ItemPackerBlock.Model model ? model : null;
        if (this.model != null) {
            this.model.setDisplay(this.itemStack);
        }
    }

    @Override
    public @Nullable Component getFilledStateText() {
        if (this.itemStack.isEmpty()) {
            return CommonComponents.EMPTY;
        }

        return Component.translatable("text.polyfactory.x_out_of_y", this.getFilledAmount(), this.getFillCapacity());
    }

    @Override
    public long getFilledAmount() {
        if (this.itemStack.getItem() instanceof BundleItem bundleItem) {
            var oc = this.itemStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).weight();
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
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (dir != null && this.getBlockState().getValue(ItemPackerBlock.FACING).getAxis() == dir.getAxis()) {
            return ItemStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null;
        }

        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return dir != null && this.getBlockState().getValue(ItemPackerBlock.FACING).getAxis() == dir.getAxis();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return this.getBlockState().getValue(ItemPackerBlock.FACING).getAxis() == side.getAxis() ? SingleStackContainer.super.getSlotsForFace(side) : new int[0];
    }

    private Storage<ItemVariant> getItemStorage() {
        if (this.cachedItemStorage != null) {
            return this.cachedItemStorage;
        }

        var storage = ItemStorage.ITEM.find(this.itemStack, ContainerItemContext.ofSingleSlot(this.inventoryStorage.getSlot(0)));

        return WrappingStorage.withModifyCallback(storage, this::runAdvancement);
    }

    private void runAdvancement() {
        if (this.level != null && FactoryUtil.getClosestPlayer(this.level, this.worldPosition, 16) instanceof ServerPlayer player) {
            TriggerCriterion.trigger(player, FactoryTriggers.ITEM_PACKER_ACCESSES);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.getStack());
        }
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity);
    }

    public int getComparatorOutput() {
        float progress;

        if (this.itemStack.has(DataComponents.BUNDLE_CONTENTS)) {
            progress = this.itemStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).weight().floatValue();
        } else {
            progress = this.getFilledAmount() / Math.max(this.getFillCapacity(), 1f);
        }

        return Mth.lerpDiscrete(Mth.clamp(progress, 0, 1), 0, 15);
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(ItemPackerBlockEntity.this.getBlockState().getBlock().getName()));
            this.setSlotRedirect(2, new PredicateLimitedSlot(ItemPackerBlockEntity.this, 0, stack -> ItemStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack)) != null));
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (ItemPackerBlockEntity.this.isRemoved() || player.position().distanceToSqr(Vec3.atCenterOf(ItemPackerBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
