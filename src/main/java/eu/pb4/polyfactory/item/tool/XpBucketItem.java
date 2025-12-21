package eu.pb4.polyfactory.item.tool;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;

public class XpBucketItem extends SimplePolymerItem {
    public XpBucketItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        for (int i = 0; i < 3; i++) {
            int xp = 3 + context.getLevel().random.nextInt(5) + context.getLevel().random.nextInt(5);
            ExperienceOrb.award((ServerLevel)context.getLevel(), context.getClickLocation(), xp);
        }

        if (context.getPlayer() != null) {
            context.getPlayer().setItemInHand(context.getHand(), ItemUtils.createFilledResult(context.getItemInHand(), context.getPlayer(), Items.BUCKET.getDefaultInstance()));
        } else {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS_SERVER;
    }
}
