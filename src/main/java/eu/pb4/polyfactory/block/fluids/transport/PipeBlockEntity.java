package eu.pb4.polyfactory.block.fluids.transport;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PipeBlockEntity extends PipeLikeBlockEntity {
    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PIPE, pos, state);
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PipeBlockEntity pipe)) {
            return;
        }
        pipe.preTick();
        if (pipe.container.isNotEmpty() || pipe.container.isNotFull()) {
            var logic = NetworkComponent.Pipe.getLogic((ServerLevel) world, pos);
            if (pipe.container.isNotEmpty()) {
                var maxFlow = logic.getWeightedMaxFlow(pos, true, pipe.fluidPush.maxPush());
                logic.runPushFlows(pos, pipe.container::isNotEmpty, (dir, strength) -> pipe.pushFluid(dir, strength, maxFlow[dir.ordinal()]));
            }
            if (pipe.container.isNotFull()) {
                logic.runPullFlows(pos, pipe.container::isNotFull, pipe::pullFluid);
            }
        }
        pipe.postTick();
    }

    @Override
    protected boolean hasDirection(Direction direction) {
        return ((PipeBaseBlock) this.getBlockState().getBlock()).checkModelDirection(this.getBlockState(), direction);
    }
}
