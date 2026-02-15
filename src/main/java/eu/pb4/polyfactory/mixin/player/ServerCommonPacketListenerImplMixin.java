package eu.pb4.polyfactory.mixin.player;

import eu.pb4.polyfactory.booklet.BookletInit;
import eu.pb4.polyfactory.item.tool.PunchCardItem;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    @Inject(method = "handleCustomClickAction", at = @At("TAIL"))
    private void onCustomClickAction(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        if (!packet.id().getNamespace().equals("polyfactory") || !(this instanceof ServerPlayerConnection connection)) {
            return;
        }

        if (packet.id().getPath().startsWith("booklet/")) {
            BookletInit.handleAction(connection.getPlayer(), packet.id().getPath().substring("booklet/".length()), packet.payload());
            return;
        }

        switch (packet.id().getPath()) {
            case "punch_card_store" -> PunchCardItem.handleClickAction(connection.getPlayer(), packet.id(), packet.payload());
        }
    }
}
