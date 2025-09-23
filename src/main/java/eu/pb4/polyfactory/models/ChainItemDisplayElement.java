package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import net.minecraft.entity.EntityPosition;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

public class ChainItemDisplayElement extends LodItemDisplayElement {
    private int updateTick;
    private Vec3d lastSyncedPosMid;

    private boolean forceSync = false;
    private boolean forceSyncMid = false;
    private final Vec3d start;
    private final Vec3d middle;
    private final Vec3d end;

    private ChainItemDisplayElement(Vec3d start, Vec3d middle, Vec3d end) {
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

        Packet<ClientPlayPacketListener> nearPacket = null;
        Packet<ClientPlayPacketListener> mediumPacket = null;
        Vec3d pos = this.getCurrentPos();

        if (!pos.equals(this.lastSyncedPos)) {
            if (this.lastSyncedPos == null || this.forceSync) {
                nearPacket = new EntityPositionSyncS2CPacket(this.getEntityId(), new EntityPosition(pos, Vec3d.ZERO, this.getYaw(), this.getPitch()), false);
                this.forceSync = false;
            } else {
                nearPacket = VirtualEntityUtils.createMovePacket(this.getEntityId(), this.lastSyncedPos, pos, this.isRotationDirty(), this.getYaw(), this.getPitch());
            }

            if (!(nearPacket instanceof EntityS2CPacket.Rotate)) {
                this.lastSyncedPos = pos;
            }
        }

        if (this.farDistanceSquared != 0 && !pos.equals(this.lastSyncedPosMid) && (updateTick++) % 10 == 0) {
            if (this.lastSyncedPosMid == null || this.forceSyncMid) {
                mediumPacket = new EntityPositionSyncS2CPacket(this.getEntityId(), new EntityPosition(pos, Vec3d.ZERO, this.getYaw(), this.getPitch()), false);
                this.forceSyncMid = false;
            } else {
                mediumPacket = VirtualEntityUtils.createMovePacket(this.getEntityId(), this.lastSyncedPosMid, pos, this.isRotationDirty(), this.getYaw(), this.getPitch());
            }

            if (!(nearPacket instanceof EntityS2CPacket.Rotate)) {
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
                    player.sendPacket(nearPacket);
                }
            } else if (d < this.farDistanceSquared) {
                if (mediumPacket != null) {
                    player.sendPacket(mediumPacket);
                }
            }
        }
    }

    @Override
    protected double getSquaredDistance(ServerPlayNetworkHandler player) {
        var pos = player.player.getPos();
        return Math.min(this.middle.squaredDistanceTo(pos), Math.min(
                this.start.squaredDistanceTo(pos),
                this.end.squaredDistanceTo(pos)
        ));
    }

    public static ChainItemDisplayElement create(ItemStack model, int updateRate, Vec3d start, Vec3d middle, Vec3d end) {
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
