package eu.pb4.polyfactory.models;

import eu.pb4.polyfactory.util.DebugData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.joml.Matrix4f;

public class BaseModel extends ElementHolder {
    private static int startTick = 0;
    private int updateTick = (startTick++) % 20;

    // Shared matrix, no reason to create a new one every time. It gets reset to identity anyway
    protected static final Matrix4f mat = new Matrix4f();

    public final int getTick() {
        return this.updateTick;
    }

    public boolean isTimeForMediumUpdate() {
        return updateTick % 2 == 0;
    }

    @Override
    public void tick() {
        super.tick();
        this.updateTick++;
    }

    @Override
    public void sendPacket(Packet<ClientPlayPacketListener> packet) {
        super.sendPacket(packet);
        DebugData.addPacketCall(this, packet);
    }

    protected double getSquaredDistance(ServerPlayNetworkHandler player) {
        return this.getPos().squaredDistanceTo(player.player.getPos());
    }
}
