package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.ModeledItem;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;

public class XpBucketItem extends ModeledItem {
    public XpBucketItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        for (int i = 0; i < 3; i++) {
            int xp = 3 + context.getWorld().random.nextInt(5) + context.getWorld().random.nextInt(5);
            ExperienceOrbEntity.spawn((ServerWorld)context.getWorld(), context.getHitPos(), xp);
        }

        if (context.getPlayer() != null) {
            context.getPlayer().setStackInHand(context.getHand(), ItemUsage.exchangeStack(context.getStack(), context.getPlayer(), Items.BUCKET.getDefaultStack()));
        } else {
            context.getStack().decrement(1);
        }

        return ActionResult.SUCCESS;
    }
}
