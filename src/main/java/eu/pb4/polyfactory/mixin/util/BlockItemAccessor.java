package eu.pb4.polyfactory.mixin.util;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockItem.class)
public interface BlockItemAccessor {
    @Invoker
    static void callCopyComponentsToBlockEntity(World world, BlockPos pos, ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Invoker
    BlockState callPlaceFromNbt(BlockPos pos, World world, ItemStack stack, BlockState state);
}
