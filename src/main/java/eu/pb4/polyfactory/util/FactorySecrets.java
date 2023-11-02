package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.stat.Stats;

import java.util.UUID;

// Small things for various people, nothing cheaty/op, so don't worry
public class FactorySecrets {
    private static final UUID TEAL_METALS = UUID.fromString("e88b3493-ea6b-42dc-94ce-1bdfda76ebbd");
    public static void onJoin(ServerPlayNetworkHandler handler, PacketSender packetSender, MinecraftServer server) {
        var player = handler.player;

        if (player.getStatHandler().getStat(Stats.CUSTOM, Stats.LEAVE_GAME) == 0) {
            if (player.getUuid().equals(TEAL_METALS)) {
                player.giveItemStack(ColoredItem.stack(FactoryItems.LAMP, 1, FactoryColors.YTTR_TEAL));
            }
        }
    }
}
