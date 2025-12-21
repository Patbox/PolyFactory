package eu.pb4.polyfactory.block.fluids.transport;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneValvePipeBlockEntity extends PipeLikeBlockEntity {
    public RedstoneValvePipeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.REDSTONE_VALVE_PIPE, pos, state);
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof RedstoneValvePipeBlockEntity pipe)) {
            return;
        }
        pipe.preTick();
        if (state.getValue(RedstoneValvePipeBlock.POWERED) == state.getValue(RedstoneValvePipeBlock.INVERTED)) {
            if (pipe.container.isNotEmpty()) {
                NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPushFlows(pos, pipe.container::isNotEmpty, pipe::pushFluid);
            }
            if (pipe.container.isNotFull()) {
                NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPullFlows(pos, pipe.container::isNotFull, pipe::pullFluid);
            }
        }
        pipe.postTick();
    }

    @Override
    protected boolean hasDirection(Direction direction) {
        var state = this.getBlockState();
        return state.getValue(RedstoneValvePipeBlock.POWERED) == state.getValue(RedstoneValvePipeBlock.INVERTED)
                && ((PipeBaseBlock) state.getBlock()).checkModelDirection(this.getBlockState(), direction);
    }
}
