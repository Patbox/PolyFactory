package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class NixieTubeControllerBlock extends GenericDirectionalDataBlock implements DataReceiver {
    public static final BooleanProperty TOP_CONNECTOR = BooleanProperty.of("top_connector");
    public static final BooleanProperty BOTTOM_CONNECTOR = BooleanProperty.of("bottom_connector");
    public NixieTubeControllerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(TOP_CONNECTOR, false).with(BOTTOM_CONNECTOR, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(TOP_CONNECTOR);
        builder.add(BOTTOM_CONNECTOR);
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data) {
        if (world.getBlockEntity(selfPos) instanceof NixieTubeControllerBlockEntity be && channel == be.channel()) {
            return be.receiveData(data);
        }
        return false;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        var be = world.getBlockEntity(pos);
        int channel = 0;
        if (be instanceof NixieTubeControllerBlockEntity blockEntity) {
            channel = blockEntity.channel();
        }
        return List.of(new ChannelReceiverDirectionNode(state.get(FACING).getOpposite(), channel));
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NixieTubeControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return type == FactoryBlockEntities.NIXIE_TUBE_CONTROLLER ? NixieTubeControllerBlockEntity::tick : null;
    }
}
