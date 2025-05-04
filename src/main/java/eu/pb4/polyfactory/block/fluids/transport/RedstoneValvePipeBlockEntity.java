package eu.pb4.polyfactory.block.fluids.transport;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class RedstoneValvePipeBlockEntity extends PipeLikeBlockEntity {
    public RedstoneValvePipeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.REDSTONE_VALVE_PIPE, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof RedstoneValvePipeBlockEntity pipe)) {
            return;
        }
        pipe.preTick();
        if (state.get(RedstoneValvePipeBlock.POWERED) == state.get(RedstoneValvePipeBlock.INVERTED)) {
            if (pipe.container.isNotEmpty()) {
                NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPushFlows(pos, pipe.container::isNotEmpty, pipe::pushFluid);
            }
            if (pipe.container.isNotFull()) {
                NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPullFlows(pos, pipe.container::isNotFull, pipe::pullFluid);
            }
        }
        pipe.postTick();
    }

    @Override
    protected boolean hasDirection(Direction direction) {
        var state = this.getCachedState();
        return state.get(RedstoneValvePipeBlock.POWERED) == state.get(RedstoneValvePipeBlock.INVERTED)
                && ((PipeBaseBlock) state.getBlock()).checkModelDirection(this.getCachedState(), direction);
    }
}
