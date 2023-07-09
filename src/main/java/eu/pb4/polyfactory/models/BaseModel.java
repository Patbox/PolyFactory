package eu.pb4.polyfactory.models;

import eu.pb4.polyfactory.util.DebugData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;

public class BaseModel extends ElementHolder {
    private static int startTick = 0;
    private int updateTick = (startTick++) % 20;

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
        DebugData.addPacketCall(packet);
        DebugData.addPacketCall(this);
    }
}
