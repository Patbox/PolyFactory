package eu.pb4.factorytools.mixin.music_disc;

import eu.pb4.factorytools.api.item.PolymerMusicDiscItem;
import eu.pb4.polymer.common.impl.CommonImplUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
    @Unique
    private final ItemDisplayElement audioSource = new ItemDisplayElement();

    @Shadow public abstract ItemStack getStack(int slot);
    @Inject(method = "<init>", at = @At("TAIL"))
    private void handleInit(BlockPos pos, BlockState state, CallbackInfo ci) {
        var holder = new ElementHolder();
        this.audioSource.setInvisible(true);
        holder.addElement(this.audioSource);
    }
    @Inject(method = "startPlaying", at = @At("TAIL"))
    private void setRecord(CallbackInfo ci) {
        removeRecord(null);

        if (this.getStack(0).getItem() instanceof PolymerMusicDiscItem c && this.getWorld() instanceof ServerWorld serverWorld) {
            var pos = this.getPos();
            ChunkAttachment.of(this.audioSource.getHolder(), serverWorld, pos);

            var packet = CommonImplUtils.createUnsafe(PlaySoundFromEntityS2CPacket.class);
            var ac = (PlaySoundFromEntityS2CPacketAccessor) packet;
            ac.setEntityId(this.audioSource.getEntityId());
            ac.setCategory(SoundCategory.RECORDS);
            ac.setSeed(serverWorld.getRandom().nextLong());
            ac.setSound(RegistryEntry.of(c.getSound()));
            ac.setVolume(4.0F);
            ac.setPitch(1);

            this.audioSource.getHolder().sendPacket(packet);
            this.audioSource.getHolder().sendPacket(
                    new GameMessageS2CPacket(Text.translatable("record.nowPlaying", c.getDescription()).formatted(Formatting.RED), true)
            );
        }
    }

    @Inject(method = "stopPlaying", at = @At("TAIL"))
    private void removeRecord(CallbackInfo ci) {
        if (this.audioSource.getHolder().getAttachment() != null) {
            this.audioSource.getHolder().destroy();
        }
    }
}
