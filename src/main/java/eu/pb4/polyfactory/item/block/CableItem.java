package eu.pb4.polyfactory.item.block;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.item.util.FireworkStarColoredItem;
import eu.pb4.polyfactory.item.util.ModeledBlockItem;
import eu.pb4.polyfactory.mixin.player.ItemUsageContextAccessor;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;

public class CableItem extends ModeledBlockItem implements FireworkStarColoredItem, DyeableItem {
    public <T extends Block & PolymerBlock> CableItem(Settings settings) {
        super(FactoryBlocks.CABLE, settings, Items.FIREWORK_STAR);
    }

    @Override
    public int getItemColor(ItemStack stack) {
        return this.hasColor(stack) ? this.getColor(stack) : 0xbbbbbb;
    }
}
