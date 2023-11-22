package eu.pb4.factorytools.mixin.music_disc;

import eu.pb4.factorytools.api.item.CustomMusicDiscItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntity {
    public JukeboxBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow public abstract ItemStack getStack(int slot);

    @Unique
    private CustomMusicDiscItem.TrackData trackData;

    @Inject(method = "startPlaying", at = @At("TAIL"))
    private void setRecord(CallbackInfo ci) {
        if (this.trackData != null) {
            this.removeRecord(null);
        }
        if (this.getStack(0).getItem() instanceof CustomMusicDiscItem c && this.getWorld() instanceof ServerWorld) {
            var request = c.getFreeTrack();

            if (request != null) {
                this.trackData = request;
                this.getWorld().playSound(null, pos, request.soundEvent(), SoundCategory.RECORDS, 4, 1);
            }
        }
    }

    @Inject(method = "stopPlaying", at = @At("TAIL"))
    private void removeRecord(CallbackInfo ci) {
        if (this.trackData != null) {
            var packet = new StopSoundS2CPacket(this.trackData.identifier(), SoundCategory.RECORDS);
            for (var player : world.getPlayers()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.networkHandler.sendPacket(packet);
                }
            }
            this.trackData.free();
            this.trackData = null;
        }
    }
}
