package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

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
    protected void saveAdditional(ValueOutput view) {
        if (!this.key1.isEmpty()) {
            view.store("key1", ItemStack.OPTIONAL_CODEC, this.key1);
        }
        if (!this.key2.isEmpty()) {
            view.store("key2", ItemStack.OPTIONAL_CODEC, this.key2);
        }
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.key1 = view.read("key1", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.key2 = view.read("key2", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        updateStack();
        super.loadAdditional(view);
    }

    private void updateStack() {
        if (this.model != null) {
            model.updateItems(this.key1, this.key2);
        }
    }

    private void updateStackWithTick() {
        updateStack();
        if (this.model != null && this.level != null) {
            model.tick();
        }
    }

    public boolean updateKey(Player player, BlockHitResult hit, ItemStack mainHandStack) {
        if (hit.getDirection() != this.getBlockState().getValue(WirelessRedstoneBlock.FACING).getOpposite()) {
            return false;
        }

        var delta = hit.getLocation().subtract(hit.getBlockPos().getCenter());
        boolean upper = hit.getDirection() == Direction.DOWN ? delta.z < 0 : hit.getDirection() == Direction.UP ? delta.z > 0 : delta.y > 0;

        var current = upper ? this.key1 : this.key2;

        if (ItemStack.isSameItemSameComponents(current, mainHandStack)) {
            return false;
        }

        if (upper) {
            this.key1 = mainHandStack.copyWithCount(1);
        } else {
            this.key2 = mainHandStack.copyWithCount(1);
        }

        updateStackWithTick();
        this.setChanged();
        return true;
    }

    public ItemStack key1() {
        return key1;
    }

    public ItemStack key2() {
        return key2;
    }

    public boolean matches(ItemStack key1, ItemStack key2) {
        return ItemStack.isSameItemSameComponents(this.key1, key1) && ItemStack.isSameItemSameComponents(this.key2, key2);
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.worldPosition).holder() instanceof ItemUpdater model ? model : null;
        this.updateStack();
    }

    public interface ItemUpdater {
        void updateItems(ItemStack key1, ItemStack key2);

        void tick();
    }
}
