package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.factorytools.api.item.FireworkStarColoredItem;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;


import java.util.List;

public class CabledBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> CabledBlockItem(T block, Settings settings) {
        super(block, settings);
    }

    @Override
    protected boolean place(ItemPlacementContext context, BlockState state) {
        var color = -2;
        if (context.getWorld().getBlockState(context.getBlockPos()).isOf(FactoryBlocks.CABLE)) {
            color = AbstractCableBlock.getColor(context.getWorld(), context.getBlockPos());
        }
        var x = super.place(context, state);
        if (color != -2 && x && context.getWorld().getBlockEntity(context.getBlockPos()) instanceof ColorProvider provider) {
            provider.setColor(color);
        }
        return x;
    }
}
