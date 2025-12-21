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
        if (pipe.container.isNotEmpty()) {
            NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPushFlows(pos, pipe.container::isNotEmpty, pipe::pushFluid);
        }
        if (pipe.container.isNotFull()) {
            NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPullFlows(pos, pipe.container::isNotFull, pipe::pullFluid);
        }
        pipe.postTick();
    }

    @Override
    protected boolean hasDirection(Direction direction) {
        return ((PipeBaseBlock) this.getBlockState().getBlock()).checkModelDirection(this.getBlockState(), direction);
    }
}
