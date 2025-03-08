package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;


public abstract class AbstractFilterItem extends ModeledItem {
    public AbstractFilterItem(Settings settings) {
        super(Items.PAPER, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity player) {
            this.openConfiguration(player, stack);
        }
        return TypedActionResult.success(stack);
    }
    public abstract void openConfiguration(ServerPlayerEntity player, ItemStack stack);


    public abstract boolean isFilterSet(ItemStack stack);

    public abstract FilterData createFilterData(ItemStack stack);
}
