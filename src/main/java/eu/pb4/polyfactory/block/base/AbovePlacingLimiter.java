package eu.pb4.polyfactory.block.base;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;

public interface AbovePlacingLimiter {
    boolean canPlaceAbove(BlockState self, ItemPlacementContext context, BlockState state);
}
