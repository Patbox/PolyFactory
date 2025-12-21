package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.factorytools.api.item.FireworkStarColoredItem;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import java.util.List;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CabledBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> CabledBlockItem(T block, Properties settings) {
        super(block, settings);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        var color = -2;
        if (context.getLevel().getBlockState(context.getClickedPos()).is(FactoryBlocks.CABLE)) {
            color = AbstractCableBlock.getColor(context.getLevel(), context.getClickedPos());
        }
        var x = super.placeBlock(context, state);
        if (color != -2 && x && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ColorProvider provider) {
            provider.setColor(color);
        }
        return x;
    }
}
