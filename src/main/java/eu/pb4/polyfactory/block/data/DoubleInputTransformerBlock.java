package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DoubleInputTransformerBlock extends DataNetworkBlock implements BlockEntityProvider, FactoryBlock, CableConnectable, DataProvider, DataReceiver, WrenchableBlock {
    public static final DirectionProperty FACING_INPUT_1 = DirectionProperty.of("facing_input_1");
    public static final DirectionProperty FACING_INPUT_2 = DirectionProperty.of("facing_input_2");
    public static final DirectionProperty FACING_OUTPUT = DirectionProperty.of("facing_output");

    private static final List<WrenchAction> WRENCH_ACTIONS = List.of(
            WrenchAction.ofAnyDirection(FACING_INPUT_1),
            WrenchAction.ofChannel("channel_input_1", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::inputChannel1, DoubleInputTransformerBlockEntity::setInputChannel1),

            WrenchAction.ofAnyDirection(FACING_INPUT_2),
            WrenchAction.ofChannel("channel_input_2", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::inputChannel2, DoubleInputTransformerBlockEntity::setInputChannel2),

            WrenchAction.ofAnyDirection(FACING_OUTPUT),
            WrenchAction.ofChannel("channel_output", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::outputChannel, DoubleInputTransformerBlockEntity::setOutputChannel),

            WrenchAction.ofBlockEntity("force_text", DoubleInputTransformerBlockEntity.class,
                x -> Boolean.toString(x.forceText()),
                (x, n) -> x.setForceText(!x.forceText())
            )
    );

    public DoubleInputTransformerBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING_INPUT_1, FACING_INPUT_2, FACING_OUTPUT);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DoubleInputTransformerBlockEntity(pos, state);
    }

    @Override
    public boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING_INPUT_1) == dir
                || state.get(FACING_INPUT_2) == dir
                || state.get(FACING_OUTPUT) == dir;
    }

    @Override
    public @Nullable DataContainer provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof DoubleInputTransformerBlockEntity be) {
            return be.lastOutput();
        }

        return null;
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        // This if statement is a mess...
        if (node instanceof DirectionNode directionNode && world.getBlockEntity(selfPos) instanceof DoubleInputTransformerBlockEntity be && (
                (channel == be.inputChannel1() && selfState.get(FACING_INPUT_1) == directionNode.direction())
                || (channel == be.inputChannel2() && selfState.get(FACING_INPUT_2) == directionNode.direction())
            ) && !(selfPos.equals(sourcePos) && sourceDir == selfState.get(FACING_OUTPUT)
                && (directionNode.direction() != sourceDir
                && FactoryNodes.DATA.getGraphView(world).getNodesAt(selfPos).filter(x -> x.getNode() == node).findFirst().map(NodeHolder::getGraphId)
                        .orElse(-1l).longValue() != FactoryNodes.DATA.getGraphView(world).getNodesAt(selfPos).filter(x -> x.getNode() instanceof DataProviderNode)
                .findFirst().map(NodeHolder::getGraphId).orElse(-2l).longValue()
        ))) {
            var input1 = channel == be.inputChannel1() && selfState.get(FACING_INPUT_1) == directionNode.direction()
                    ? be.setLastInput1(data) : be.lastInput1();
            var input2 = channel == be.inputChannel2() && selfState.get(FACING_INPUT_2) == directionNode.direction()
                    ? be.setLastInput2(data) : be.lastInput2();

            sendData(world, selfState.get(FACING_OUTPUT), selfPos, this.transformData(input1, input2, world, selfPos, selfState, be));
            return true;
        }

        return false;
    }

    protected DataContainer transformData(DataContainer input1, DataContainer input2, ServerWorld world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be) {
        if (be.forceText()) {
            return new StringData(input1.asString() + input2.asString());
        }

        return DataContainer.of(input1.asLong() + input2.asLong());
    }

    public int sendData(WorldAccess world, Direction direction, BlockPos selfPos, DataContainer data) {
        if (data != null && world instanceof ServerWorld serverWorld && world.getBlockEntity(selfPos) instanceof DoubleInputTransformerBlockEntity be) {
            be.setLastOutput(data);
            return NetworkComponent.Data.getLogic(serverWorld, selfPos,
                    x -> x.getNode() instanceof ChannelProviderDirectionNode p && p.channel() == be.outputChannel() && p.direction() == direction)
                    .pushDataUpdate(selfPos, be.outputChannel(), data, direction);
        }
        return 0;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            return List.of(
                    new ChannelReceiverDirectionNode(state.get(FACING_INPUT_1), be.inputChannel1()),
                    new ChannelReceiverDirectionNode(state.get(FACING_INPUT_2), be.inputChannel2()),
                    new ChannelProviderDirectionNode(state.get(FACING_OUTPUT), be.outputChannel())
            );
        }
        return List.of();
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return WRENCH_ACTIONS;
    }
}
