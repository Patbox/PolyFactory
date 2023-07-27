package eu.pb4.polyfactory.models;

import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayPingS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Consumer;

public class FastItemDisplayElement extends LodItemDisplayElement {
    private final ObjectOpenCustomHashSet<ServerPlayNetworkHandler> fastPlayers = new ObjectOpenCustomHashSet<>(Util.identityHashStrategy());
    private ItemStack fastItemStack = ItemStack.EMPTY;
    private int fastItemDistance = Integer.MAX_VALUE;
    private Packet<ClientPlayPacketListener> fastPacket;
    private Packet<ClientPlayPacketListener> slowPacket;

    public FastItemDisplayElement(ItemStack stack) {
        super();
        this.setItem(stack);
    }

    public FastItemDisplayElement() {
        super();
    }

    public void setFastItem(ItemStack stack, int distance) {
        this.fastItemStack = stack;
        this.fastPlayers.clear();
        this.getDataTracker().setDirty(DisplayTrackedData.Item.ITEM, true);
        this.fastItemDistance = distance * distance;
        this.fastPacket = new EntityTrackerUpdateS2CPacket(this.getEntityId(), List.of(DataTracker.SerializedEntry.of(DisplayTrackedData.Item.ITEM, this.fastItemStack)));
    }

    @Override
    public void setItem(ItemStack stack) {
        super.setItem(stack);
        this.slowPacket = new EntityTrackerUpdateS2CPacket(this.getEntityId(), List.of(DataTracker.SerializedEntry.of(DisplayTrackedData.Item.ITEM, this.getItem())));
    }

    @Override
    protected void sendChangedTrackerEntries(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        super.sendChangedTrackerEntries(player, packetConsumer);

        if (isEnabled) {
            var d = this.getSquaredDistance(player.networkHandler);
            if (d > this.fastItemDistance) {
                packetConsumer.accept(this.fastPacket);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (isEnabled) {
            for (var player : this.getHolder().getWatchingPlayers()) {
                var d = this.getSquaredDistance(player);

                if (d > this.fastItemDistance) {
                    if (this.fastPlayers.add(player)) {
                        player.sendPacket(this.fastPacket);
                    }
                } else {
                    if (this.fastPlayers.remove(player)) {
                        player.sendPacket(this.slowPacket);
                    }
                }
            }
        } else {
            for (var player : this.fastPlayers) {
                player.sendPacket(this.slowPacket);
            }
            this.fastPlayers.clear();
        }
    }
}
