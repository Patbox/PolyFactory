package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ChainItemDisplayElement extends LodItemDisplayElement {
    private int updateTick;
    private Vec3 lastSyncedPosMid;

    private boolean forceSync = false;
    private boolean forceSyncMid = false;
    private final Vec3 start;
    private final Vec3 middle;
    private final Vec3 end;

    private ChainItemDisplayElement(Vec3 start, Vec3 middle, Vec3 end) {
        super();
        this.start = start;
        this.middle = middle;
        this.end = end;
        this.addLodData(DisplayTrackedData.TELEPORTATION_DURATION);
    }

    public void forceSync() {
        forceSync = true;
        forceSyncMid = true;
    }

    @Override
    protected void sendPositionUpdates() {
        if (this.getHolder() == null) {
            return;
        }

        Packet<ClientGamePacketListener> nearPacket = null;
        Packet<ClientGamePacketListener> mediumPacket = null;
        Vec3 pos = this.getCurrentPos();

        if (!pos.equals(this.lastSyncedPos)) {
            if (this.lastSyncedPos == null || this.forceSync) {
                nearPacket = new ClientboundEntityPositionSyncPacket(this.getEntityId(), new PositionMoveRotation(pos, Vec3.ZERO, this.getYaw(), this.getPitch()), false);
                this.forceSync = false;
            } else {
                nearPacket = VirtualEntityUtils.createMovePacket(this.getEntityId(), this.lastSyncedPos, pos, this.isRotationDirty(), this.getYaw(), this.getPitch());
            }

            if (!(nearPacket instanceof ClientboundMoveEntityPacket.Rot)) {
                this.lastSyncedPos = pos;
            }
        }

        if (this.farDistanceSquared != 0 && !pos.equals(this.lastSyncedPosMid) && (updateTick++) % 10 == 0) {
            if (this.lastSyncedPosMid == null || this.forceSyncMid) {
                mediumPacket = new ClientboundEntityPositionSyncPacket(this.getEntityId(), new PositionMoveRotation(pos, Vec3.ZERO, this.getYaw(), this.getPitch()), false);
                this.forceSyncMid = false;
            } else {
                mediumPacket = VirtualEntityUtils.createMovePacket(this.getEntityId(), this.lastSyncedPosMid, pos, this.isRotationDirty(), this.getYaw(), this.getPitch());
            }

            if (!(nearPacket instanceof ClientboundMoveEntityPacket.Rot)) {
                this.lastSyncedPosMid = pos;
            }
        }

        if (nearPacket == null && mediumPacket == null) {
            return;
        }

        for (var player : this.getHolder().getWatchingPlayers()) {
            var d = this.getSquaredDistance(player);
            if (d < this.nearDistanceSquared) {
                if (nearPacket != null) {
                    player.send(nearPacket);
                }
            } else if (d < this.farDistanceSquared) {
                if (mediumPacket != null) {
                    player.send(mediumPacket);
                }
            }
        }
    }

    @Override
    protected double getSquaredDistance(ServerGamePacketListenerImpl player) {
        var pos = player.player.position();
        return Math.min(this.middle.distanceToSqr(pos), Math.min(
                this.start.distanceToSqr(pos),
                this.end.distanceToSqr(pos)
        ));
    }

    public static ChainItemDisplayElement create(ItemStack model, int updateRate, Vec3 start, Vec3 middle, Vec3 end) {
        var element = new ChainItemDisplayElement(start, middle, end);
        element.setDisplaySize(2.0F, 2.0F);
        element.setViewRange(0.8F);
        element.setItemDisplayContext(ItemDisplayContext.FIXED);
        element.setTeleportDuration(updateRate);
        element.setInterpolationDuration(updateRate);
        element.setInvisible(true);
        element.setItem(model);

        element.nearDistanceSquared = 28 * 28;
        element.farDistanceSquared = 0;
        return element;
    }
}
