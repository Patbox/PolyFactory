package eu.pb4.polyfactory.block.base;

import eu.pb4.polyfactory.block.mechanical.machines.MinerBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public abstract class LockableBlockEntity extends BlockEntity {
    private ContainerLock lock;
    @Nullable
    private Text customName;

    protected LockableBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        this.lock = ContainerLock.EMPTY;
    }

    public static boolean checkUnlocked(PlayerEntity player, ContainerLock lock, Text containerName) {
        if (!player.isSpectator() && !lock.canOpen(player.getMainHandStack())) {
            player.sendMessage(Text.translatable("container.isLocked", containerName), true);
            player.playSound(SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return false;
        } else {
            return true;
        }
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.lock = ContainerLock.fromNbt(nbt);
        if (nbt.contains("CustomName", 8)) {
            this.customName = Text.Serialization.fromJson(nbt.getString("CustomName"));
        }
        this.readNbtMixin(nbt);
    }

    private void readNbtMixin(NbtCompound nbt) {}

    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        this.lock.writeNbt(nbt);
        if (this.customName != null) {
            nbt.putString("CustomName", Text.Serialization.toJsonString(this.customName));
        }
        this.writeNbtMixin(nbt);
    }

    private void writeNbtMixin(NbtCompound nbt) {}

    public Text getName() {
        return this.customName != null ? this.customName : this.getContainerName();
    }

    public Text getDisplayName() {
        return this.getName();
    }

    @Nullable
    public Text getCustomName() {
        return this.customName;
    }

    public void setCustomName(Text customName) {
        this.customName = customName;
    }

    protected Text getContainerName() {
        return this.getCachedState().getBlock().getName();
    }

    public boolean checkUnlocked(PlayerEntity player) {
        return checkUnlocked(player, this.lock, this.getDisplayName()) && checkUnlockedMixin();
    }

    private boolean checkUnlockedMixin() {
        return true;
    }

    @Nullable
    public final void openGui(ServerPlayerEntity playerEntity) {
        if (this.checkUnlocked(playerEntity)) {
            this.createGui(playerEntity);
        }
    }

    protected void createGui(ServerPlayerEntity playerEntity) {};
}
