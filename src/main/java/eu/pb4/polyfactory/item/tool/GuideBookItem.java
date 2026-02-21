package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.booklet.BookletInit;
import eu.pb4.polyfactory.booklet.BookletOpenState;
import eu.pb4.polyfactory.booklet.BookletUtil;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

import static eu.pb4.polyfactory.ModInit.id;

public class GuideBookItem extends SimplePolymerItem {
    public GuideBookItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var id = player.getItemInHand(hand).get(FactoryDataComponents.BOOKLET_PAGE);
        if (player instanceof ServerPlayer serverPlayer && id != null) {
            TriggerCriterion.trigger(serverPlayer, FactoryTriggers.GUIDEBOOK);
            BookletUtil.openPage(serverPlayer, id, BookletOpenState.DEFAULT);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public Component getName(ItemStack stack) {
        var id = stack.get(FactoryDataComponents.BOOKLET_PAGE);
        if (id != null) {
            var page = BookletUtil.getPage(id, "en_us");
            if (page != null && page.info().description().isPresent()) {
               return page.info().getExternalTitle();
            }
        }
        return super.getName(stack);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        var id = stack.get(FactoryDataComponents.BOOKLET_PAGE);
        if (id != null) {
            var page = BookletUtil.getPage(id, "en_us");
            if (page != null && page.info().description().isPresent()) {
                tooltipAdder.accept(Component.empty().append(page.info().description().orElseThrow()).withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
    }
}
