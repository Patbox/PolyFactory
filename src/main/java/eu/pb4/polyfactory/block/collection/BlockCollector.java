package eu.pb4.polyfactory.block.collection;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class BlockCollector {
    public static Result collect(ServerLevel world, BlockPos start) {
        var set = new HashSet<BlockPos>();
        var startMut = start.mutable();
        var endMut = start.mutable();
        var mut = start.mutable();

        collectRecursive(world, world.getBlockState(mut), set, startMut, endMut, mut);

        return new Result(endMut.getX() - startMut.getX(), endMut.getY() - startMut.getY(), endMut.getZ() - startMut.getZ(), set);
    }

    private static void collectRecursive(ServerLevel world, BlockState state, HashSet<BlockPos> set, BlockPos.MutableBlockPos startMut, BlockPos.MutableBlockPos endMut, BlockPos.MutableBlockPos mut) {
        if (set.contains(mut) || state.getPistonPushReaction() == PushReaction.BLOCK || state.getBlock() instanceof EntityBlock) {
            return;
        }

        set.add(mut.immutable());
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

        boolean sticky = state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);

        for (var dir : Direction.values()) {
            var nextState = world.getBlockState(mut.move(dir));
            if (sticky || nextState.is(Blocks.SLIME_BLOCK) || nextState.is(Blocks.HONEY_BLOCK)) {
                collectRecursive(world, nextState, set, startMut, endMut, mut);
            }
            mut.move(dir, -1);
        }
    }


    public record Result(int sizeX, int sizeY, int sizeZ, Set<BlockPos> pos) {};
}
