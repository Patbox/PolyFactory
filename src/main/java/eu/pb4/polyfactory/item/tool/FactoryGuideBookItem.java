package eu.pb4.polyfactory.item.tool;

import eu.pb4.booklet.api.item.GuideBookItem;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class FactoryGuideBookItem extends GuideBookItem {
    public FactoryGuideBookItem(Properties settings) {
        super(settings, id("main_page"));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            TriggerCriterion.trigger(serverPlayer, FactoryTriggers.GUIDEBOOK);
        }
        return super.use(level, player, hand);
    }
}
