package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.other.FactoryBiomeTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

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

    public static float get(World world, BlockPos pos) {
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

    public static float getReceived(World world, BlockPos pos) {
        return get(world, pos.down());
    }
    public static float forBlockState(BlockState state) {
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

    public static float forBiome(RegistryEntry<Biome> biome) {
        if (biome.isIn(FactoryBiomeTags.TEMPERATURE_REALLY_HOT)) {
            return BIOME_REALLY_HOT;
        } else if (biome.isIn(FactoryBiomeTags.TEMPERATURE_REALLY_COLD)) {
            return BIOME_REALLY_COLD;
        } else if (biome.isIn(FactoryBiomeTags.TEMPERATURE_HOT)) {
            return BIOME_HOT;
        } else if (biome.isIn(FactoryBiomeTags.TEMPERATURE_COLD)) {
            return BIOME_COLD;
        } else {
            return NEUTRAL;
        }
    }
}
