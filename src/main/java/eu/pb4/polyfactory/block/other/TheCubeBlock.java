package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.factorytools.api.block.CustomPistonBehavior;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class TheCubeBlock extends MultiBlock implements CustomPistonBehavior {
    public TheCubeBlock(Settings settings) {
        super(2, 2, 2, settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        this.settings.pistonBehavior(PistonBehavior.NORMAL);
        super.appendProperties(builder);
    }

    @Override
    public boolean isSticky(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return state;
    }

    @Override
    public boolean isStickyToNeighbor(World world, BlockPos pos, BlockState state, BlockPos neighborPos, BlockState neighborState, Direction dir, Direction moveDir) {
        if (state.getBlock() != neighborState.getBlock()) {
            return CustomPistonBehavior.isVanillaSticky(neighborState);
        }

        for (var axis : Direction.Axis.VALUES) {
            var prop = switch (axis) {
                case X -> partX;
                case Y -> partY;
                case Z -> partZ;
            };
            if (axis != dir.getAxis()) {
                if (prop != null && state.get(prop) != neighborState.get(prop)) {
                    return false;
                }
            } else if (prop == null || neighborState.get(prop) - state.get(prop) != dir.getDirection().offset()) {
                return false;
            }
        }

        return true;
    }
}
