package eu.pb4.polyfactory.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BlockHeat {
    public static final float LAVA = 0.85f;
    public static final float CAMPFIRE = 0.45f;
    public static final float MAGMA = 0.45f;
    public static final float FIRE = 0.45f;
    public static final float TORCH = 0.15f;
    public static final float EXPERIENCE = 0.1f;
    public static final float NEUTRAL = 0f;
    public static final float SNOW = -0.08f;
    public static final float ICE = -0.1f;

    public static float get(World world, BlockPos pos) {
        return get(world.getBlockState(pos));
    }
    public static float getReceived(World world, BlockPos pos) {
        return get(world, pos.down());
    }
    public static float get(BlockState state) {
        if (state.isIn(BlockTags.CAMPFIRES) && state.get(CampfireBlock.LIT)) {
            return CAMPFIRE;
        } else if (state.isOf(Blocks.MAGMA_BLOCK)) {
            return MAGMA;
        }  else if (state.isIn(BlockTags.FIRE)) {
            return FIRE;
        } else if (state.isOf(Blocks.LAVA)) {
            return LAVA;
        } else if (state.isOf(Blocks.TORCH)) {
            return TORCH;
        } else {
            return NEUTRAL;
        }
    }
}
