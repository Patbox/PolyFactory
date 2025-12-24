package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

import static eu.pb4.polyfactory.util.FactoryUtil.id;


public class PunchCardItem extends SimplePolymerItem {
    public PunchCardItem(Properties settings) {
        super(settings.component(FactoryDataComponents.PUNCH_CARD_DATA, List.of()).stacksTo(1));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if (user instanceof ServerPlayer player) {
            var compound = new CompoundTag();
            compound.putString("hand", hand.name());
            player.openDialog(Holder.direct(new NoticeDialog(
                    new CommonDialogData(stack.getHoverName(), Optional.empty(), true, false, DialogAction.CLOSE, List.of(),
                            List.of(new Input("data", new TextInput(300, Component.empty(), false,
                                    String.join("\n", stack.getOrDefault(FactoryDataComponents.PUNCH_CARD_DATA, List.of())), Integer.MAX_VALUE,
                                    Optional.of(new TextInput.MultilineOptions(Optional.empty(), Optional.of(170))))))),
                    new ActionButton(new CommonButtonData(CommonComponents.GUI_DONE, 150), Optional.of(new CustomAll(id("punch_card_store"), Optional.of(compound))))
            )));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public static void handleClickAction(ServerPlayer player, Identifier id, Optional<Tag> payload) {
        var tag = payload.flatMap(Tag::asCompound);
        if (tag.isEmpty()) return;
        var data = tag.orElseThrow();
        var hand = data.getStringOr("hand", "").equals(InteractionHand.OFF_HAND.name()) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        var text = data.getStringOr("data", "");

        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof PunchCardItem)) return;
        stack.set(FactoryDataComponents.PUNCH_CARD_DATA, List.of(text.split("\n")));
        player.swing(hand, true);
    }
}
