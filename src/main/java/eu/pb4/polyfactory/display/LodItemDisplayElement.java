package eu.pb4.polyfactory.display;

import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DataTrackerLike;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import eu.pb4.polymer.virtualentity.api.tracker.SimpleDataTracker;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class LodItemDisplayElement extends ItemDisplayElement {
    public final DataTrackerLike nearTracker = new SimpleDataTracker(this.getEntityType());
    public final DataTrackerLike mediumTracker = new SimpleDataTracker(this.getEntityType());

    public LodItemDisplayElement(ItemStack stack) {
        super();
        this.getDataTracker().set(DisplayTrackedData.Item.ITEM, stack);
    }

    public LodItemDisplayElement() {
        super();
    }

    @Override
    protected DataTrackerLike createDataTracker() {
        return new DataTrackerLike() {
            @Override
            public <T> @Nullable T get(TrackedData<T> data) {
                return nearTracker.get(data);
            }

            @Override
            public <T> void set(TrackedData<T> key, T value, boolean forceDirty) {
                nearTracker.set(key, value, forceDirty);
                mediumTracker.set(key, value, forceDirty);
            }

            @Override
            public <T> void setDirty(TrackedData<T> key, boolean isDirty) {
                set(key, get(key), isDirty);
            }

            @Override
            public boolean isDirty() {
                return nearTracker.isDirty();
            }

            @Override
            public @Nullable List<DataTracker.SerializedEntry<?>> getDirtyEntries() {
                return nearTracker.getDirtyEntries();
            }

            @Override
            public @Nullable List<DataTracker.SerializedEntry<?>> getChangedEntries() {
                return nearTracker.getChangedEntries();
            }

            @Override
            public boolean isEmpty() {
                return nearTracker.isEmpty();
            }
        };
    }

    @Override
    protected void sendChangedTrackerEntries(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        super.sendChangedTrackerEntries(player, packetConsumer);
    }

    @Override
    protected void sendTrackerUpdates() {
        Packet<ClientPlayPacketListener> nearPacket = null;
        Packet<ClientPlayPacketListener> mediumPacket = null;
        if (this.nearTracker.isDirty()) {
            nearPacket = new EntityTrackerUpdateS2CPacket(this.getEntityId(), this.nearTracker.getDirtyEntries());
        }

        if (this.mediumTracker.isDirty() && ((LodElementHolder) this.getHolder()).isTimeForMediumUpdate()) {
            mediumPacket = new EntityTrackerUpdateS2CPacket(this.getEntityId(), this.mediumTracker.getDirtyEntries());
        }

        for (var player : this.getHolder().getWatchingPlayers()) {
            var d = player.player.getPos().squaredDistanceTo(this.getHolder().getPos());
            if (d < 32 * 32) {
                if (nearPacket != null) {
                    player.sendPacket(nearPacket);
                }
            } else if (d < 80 * 80) {
                if (mediumPacket != null) {
                    player.sendPacket(mediumPacket);
                }
            }
        }
    }
}
