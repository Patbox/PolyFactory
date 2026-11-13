package eu.pb4.polyfactory.util;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

public class ClientPlayerInterpolator {

    public static void interpolateTo(ServerPlayer player, Vec3 deltaPos, float rotY, float rotX) {
        player.connection.send(new ClientboundPlayerPositionPacket(player.getId(),
        //player.connection.send(new ClientboundTeleportEntityPacket(player.getId(),
                new PositionMoveRotation(deltaPos, Vec3.ZERO, rotY, rotX),
                Relative.ALL//, false
        ));
    }
}
