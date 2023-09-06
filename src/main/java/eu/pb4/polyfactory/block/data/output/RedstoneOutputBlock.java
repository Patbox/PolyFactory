package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;

import java.util.Collection;
import java.util.List;

public class RedstoneOutputBlock extends GenericDirectionalDataBlock implements DataReceiver {
    public static final IntProperty POWER = IntProperty.of("power", 0, 15);

    public RedstoneOutputBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWER, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWER);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return direction.getOpposite() == state.get(FACING) ? state.get(POWER) : 0;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelReceiverDirectionNode(state.get(FACING).getOpposite(), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data) {
        var val = (int) MathHelper.clamp(data.asLong(), 0, 15);
        if (selfState.get(POWER) != val) {
            world.setBlockState(selfPos, selfState.with(POWER, val));
        }

        return true;
    }
}
