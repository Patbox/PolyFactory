package eu.pb4.polyfactory.mixin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockItem.class)
public interface BlockItemAccessor {
    @Invoker
    static void callUpdateBlockEntityComponents(Level world, BlockPos pos, ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Invoker
    BlockState callUpdateBlockStateFromTag(BlockPos pos, Level world, ItemStack stack, BlockState state);
}
