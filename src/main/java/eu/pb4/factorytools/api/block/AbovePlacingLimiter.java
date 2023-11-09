package eu.pb4.factorytools.api.block;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;

public interface AbovePlacingLimiter {
    boolean canPlaceAbove(BlockState self, ItemPlacementContext context, BlockState state);
}
