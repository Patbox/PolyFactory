package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;


public abstract class AbstractFilterItem extends SimplePolymerItem {
    public AbstractFilterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity player) {
            this.openConfiguration(player, stack);
        }
        return ActionResult.SUCCESS_SERVER;
    }
    public abstract void openConfiguration(ServerPlayerEntity player, ItemStack stack);


    public abstract boolean isFilterSet(ItemStack stack);

    public abstract FilterData createFilterData(ItemStack stack);
}
