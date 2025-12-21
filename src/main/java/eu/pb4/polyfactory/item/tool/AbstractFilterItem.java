package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public abstract class AbstractFilterItem extends SimplePolymerItem {
    public AbstractFilterItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if (user instanceof ServerPlayer player) {
            this.openConfiguration(player, stack);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
    public abstract void openConfiguration(ServerPlayer player, ItemStack stack);


    public abstract boolean isFilterSet(ItemStack stack);

    public abstract FilterData createFilterData(ItemStack stack);
}
