package eu.pb4.polyfactory.mixin.compat;

import com.github.fabricservertools.htm.HTMContainerLock;
import com.github.fabricservertools.htm.api.LockableObject;
import eu.pb4.polyfactory.block.other.LockableBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("OverwriteAuthorRequired")
@Mixin(LockableBlockEntity.class)
public abstract class htm_LockableBlockEntityMixin implements LockableObject {
    public HTMContainerLock htmContainerLock = new HTMContainerLock();

    @Overwrite
    private void readNbtMixin(NbtCompound nbt) {
        htmContainerLock.fromTag(nbt);
    }

    @Overwrite
    private void writeNbtMixin(NbtCompound nbt) {
        htmContainerLock.toTag(nbt);
    }


    @Overwrite
    private boolean checkUnlockedMixin(PlayerEntity player) {
        return player instanceof ServerPlayerEntity serverPlayer && htmContainerLock.canOpen(serverPlayer);
    }

    @Override
    public HTMContainerLock getLock() {
        return htmContainerLock;
    }

    @Override
    public void setLock(HTMContainerLock lock) {
        htmContainerLock = lock;
    }
}
