package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

@SuppressWarnings("UnstableApiUsage")
public class WirelessRedstoneBlockEntity extends LockableBlockEntity implements BlockEntityExtraListener {
    private ItemStack key1 = ItemStack.EMPTY;
    private ItemStack key2 = ItemStack.EMPTY;
    private ItemUpdater model;

    public WirelessRedstoneBlockEntity(BlockPos pos, BlockState state) {
        this(FactoryBlockEntities.WIRELESS_REDSTONE, pos, state);
    }

    protected WirelessRedstoneBlockEntity(BlockEntityType<? extends WirelessRedstoneBlockEntity> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    protected void writeData(WriteView view) {
        if (!this.key1.isEmpty()) {
            view.put("key1", ItemStack.OPTIONAL_CODEC, this.key1);
        }
        if (!this.key2.isEmpty()) {
            view.put("key2", ItemStack.OPTIONAL_CODEC, this.key2);
        }
        super.writeData(view);
    }

    @Override
    public void readData(ReadView view) {
        this.key1 = view.read("key1", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.key2 = view.read("key2", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        updateStack();
        super.readData(view);
    }

    private void updateStack() {
        if (this.model != null) {
            model.updateItems(this.key1, this.key2);
        }
    }

    private void updateStackWithTick() {
        updateStack();
        if (this.model != null && this.world != null) {
            model.tick();
        }
    }

    public boolean updateKey(PlayerEntity player, BlockHitResult hit, ItemStack mainHandStack) {
        if (hit.getSide() != this.getCachedState().get(WirelessRedstoneBlock.FACING).getOpposite()) {
            return false;
        }

        var delta = hit.getPos().subtract(hit.getBlockPos().toCenterPos());
        boolean upper = hit.getSide() == Direction.DOWN ? delta.z < 0 : hit.getSide() == Direction.UP ? delta.z > 0 : delta.y > 0;

        var current = upper ? this.key1 : this.key2;

        if (ItemStack.areItemsAndComponentsEqual(current, mainHandStack)) {
            return false;
        }

        if (upper) {
            this.key1 = mainHandStack.copyWithCount(1);
        } else {
            this.key2 = mainHandStack.copyWithCount(1);
        }

        updateStackWithTick();
        this.markDirty();
        return true;
    }

    public ItemStack key1() {
        return key1;
    }

    public ItemStack key2() {
        return key2;
    }

    public boolean matches(ItemStack key1, ItemStack key2) {
        return ItemStack.areItemsAndComponentsEqual(this.key1, key1) && ItemStack.areItemsAndComponentsEqual(this.key2, key2);
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.pos).holder() instanceof ItemUpdater model ? model : null;
        this.updateStack();
    }

    public interface ItemUpdater {
        void updateItems(ItemStack key1, ItemStack key2);

        void tick();
    }
}
