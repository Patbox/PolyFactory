package eu.pb4.polyfactory.block.collection;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public class BlockCollector {
    public static Result collect(ServerWorld world, BlockPos start) {
        var set = new HashSet<BlockPos>();
        var startMut = start.mutableCopy();
        var endMut = start.mutableCopy();
        var mut = start.mutableCopy();

        collectRecursive(world, world.getBlockState(mut), set, startMut, endMut, mut);

        return new Result(endMut.getX() - startMut.getX(), endMut.getY() - startMut.getY(), endMut.getZ() - startMut.getZ(), set);
    }

    private static void collectRecursive(ServerWorld world, BlockState state, HashSet<BlockPos> set, BlockPos.Mutable startMut, BlockPos.Mutable endMut, BlockPos.Mutable mut) {
        if (set.contains(mut) || state.getPistonBehavior() == PistonBehavior.BLOCK || state.getBlock() instanceof BlockEntityProvider) {
            return;
        }

        set.add(mut.toImmutable());
        startMut.set(
                Math.min(startMut.getX(), mut.getX()),
                Math.min(startMut.getY(), mut.getY()),
                Math.min(startMut.getZ(), mut.getZ())
        );
        endMut.set(
                Math.max(startMut.getX(), mut.getX()),
                Math.max(startMut.getY(), mut.getY()),
                Math.max(startMut.getZ(), mut.getZ())
        );

        boolean sticky = state.isOf(Blocks.SLIME_BLOCK) || state.isOf(Blocks.HONEY_BLOCK);

        for (var dir : Direction.values()) {
            var nextState = world.getBlockState(mut.move(dir));
            if (sticky || nextState.isOf(Blocks.SLIME_BLOCK) || nextState.isOf(Blocks.HONEY_BLOCK)) {
                collectRecursive(world, nextState, set, startMut, endMut, mut);
            }
            mut.move(dir, -1);
        }
    }


    public record Result(int sizeX, int sizeY, int sizeZ, Set<BlockPos> pos) {};
}
