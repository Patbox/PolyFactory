package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.other.FactoryBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

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
    public static final float BIOME_REALLY_HOT = 0.15f;
    public static final float BIOME_HOT = 0.08f;
    public static final float BIOME_REALLY_COLD = -0.15f;
    public static final float BIOME_COLD = -0.08f;

    public static float get(Level world, BlockPos pos) {
        var x = forBlockState(world.getBlockState(pos));
        var y = forBiome(world.getBiome(pos));

        if (y == NEUTRAL) {
            return x;
        } else if (x == NEUTRAL) {
            return y;
        } else if (x > NEUTRAL && y > NEUTRAL) {
            return Math.max(x, y);
        } else if (x < NEUTRAL && y < NEUTRAL) {
            return Math.min(x, y);
        } else {
            return x;
        }

    }

    public static float getReceived(Level world, BlockPos pos) {
        return get(world, pos.below());
    }
    public static float forBlockState(BlockState state) {
        if (state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT)) {
            return CAMPFIRE;
        } else if (state.is(Blocks.MAGMA_BLOCK)) {
            return MAGMA;
        }  else if (state.is(BlockTags.FIRE)) {
            return FIRE;
        } else if (state.is(Blocks.LAVA)) {
            return LAVA;
        } else if (state.is(Blocks.TORCH)) {
            return TORCH;                           
        } else if (state.is(BlockTags.SNOW)) {
            return SNOW;
        } else if (state.is(BlockTags.ICE)) {
            return ICE;
        } else {
            return NEUTRAL;
        }
    }

    public static float forBiome(Holder<Biome> biome) {
        if (biome.is(FactoryBiomeTags.TEMPERATURE_REALLY_HOT)) {
            return BIOME_REALLY_HOT;
        } else if (biome.is(FactoryBiomeTags.TEMPERATURE_REALLY_COLD)) {
            return BIOME_REALLY_COLD;
        } else if (biome.is(FactoryBiomeTags.TEMPERATURE_HOT)) {
            return BIOME_HOT;
        } else if (biome.is(FactoryBiomeTags.TEMPERATURE_COLD)) {
            return BIOME_COLD;
        } else {
            return NEUTRAL;
        }
    }
}
