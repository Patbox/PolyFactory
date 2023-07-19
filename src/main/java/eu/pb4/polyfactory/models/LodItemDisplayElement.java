package eu.pb4.polyfactory.models;

import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.util.DebugData;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DataTrackerLike;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import eu.pb4.polymer.virtualentity.api.tracker.SimpleDataTracker;
import net.minecraft.client.render.model.json.ModelTransformationMode;
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
    private int updateTick = 0;
    private double nearDistanceSquared = 50 * 50;
    private float farDistanceSquared = 90 * 90;

    public LodItemDisplayElement(ItemStack stack) {
        super();
        this.getDataTracker().set(DisplayTrackedData.Item.ITEM, stack);
    }

    public LodItemDisplayElement() {
        super();
    }

    public static LodItemDisplayElement createSimple(ItemStack model) {
        var element = createSimple();
        element.setItem(model);
        return element;
    }

    public static LodItemDisplayElement createSimple(ItemStack model, int updateRate, float qualityMultiplier, float farQualityDistanceMultiplier) {
        var element = createSimple(model, updateRate);
        element.nearDistanceSquared = 50 * 50 * qualityMultiplier * qualityMultiplier;
        element.farDistanceSquared = 90 * 90 * farQualityDistanceMultiplier * farQualityDistanceMultiplier;
        return element;
    }

    public static LodItemDisplayElement createSimple(ItemStack model, int updateRate, float qualityMultiplier) {
        var element = createSimple(model, updateRate);
        element.nearDistanceSquared = 50 * 50 * qualityMultiplier * qualityMultiplier;
        return element;
    }
    public static LodItemDisplayElement createSimple(ItemStack model, int updateRate) {
        var element = createSimple(model);
        element.setInterpolationDuration(updateRate);
        return element;
    }

    public static LodItemDisplayElement createSimple() {
        var element = new LodItemDisplayElement();
        element.setDisplaySize(2, 2);
        element.setViewRange(0.8f);
        element.setModelTransformation(ModelTransformationMode.FIXED);
        element.setInvisible(true);
        return element;
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
                if (key != DisplayTrackedData.START_INTERPOLATION) {
                    mediumTracker.set(key, value, forceDirty);
                }
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

        if (this.mediumTracker.isDirty() && (updateTick++) % 10 == 0) {
            mediumPacket = new EntityTrackerUpdateS2CPacket(this.getEntityId(), this.mediumTracker.getDirtyEntries());
        }

        if (nearPacket == null && mediumPacket == null) {
            return;
        }

        boolean sendOnce = false;

        for (var player : this.getHolder().getWatchingPlayers()) {
            var d = player.player.getPos().squaredDistanceTo(this.getHolder().getPos());
            if (d < this.nearDistanceSquared) {
                if (nearPacket != null) {
                    player.sendPacket(nearPacket);
                    sendOnce = true;
                }
            } else if (d < this.farDistanceSquared) {
                if (mediumPacket != null) {
                    player.sendPacket(mediumPacket);
                    sendOnce = true;
                }
            }
        }

        if (sendOnce) {
            DebugData.addPacketCall(this.getHolder());
            DebugData.addPacketCall(nearPacket != null ? nearPacket : mediumPacket);
        }
    }
}
