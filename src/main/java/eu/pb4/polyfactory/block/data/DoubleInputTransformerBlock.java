package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.base.AxisAndFacingBlock;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public abstract class DoubleInputTransformerBlock extends DataNetworkBlock implements BlockEntityProvider, FactoryBlock, CableConnectable, DataProvider, DataReceiver, WrenchableBlock {
    public static final EnumProperty<Direction> FACING_INPUT_1 = EnumProperty.of("facing_input_1", Direction.class);
    public static final EnumProperty<Direction> FACING_INPUT_2 = EnumProperty.of("facing_input_2", Direction.class);
    public static final EnumProperty<Direction> FACING_OUTPUT = EnumProperty.of("facing_output", Direction.class);

     protected static final List<WrenchAction> WRENCH_ACTIONS = List.of(
            WrenchAction.ofDirection(FACING_INPUT_1),
            WrenchAction.ofChannel("channel_input_1", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::inputChannel1, DoubleInputTransformerBlockEntity::setInputChannel1),

            WrenchAction.ofDirection(FACING_INPUT_2),
            WrenchAction.ofChannel("channel_input_2", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::inputChannel2, DoubleInputTransformerBlockEntity::setInputChannel2),

            WrenchAction.ofDirection(FACING_OUTPUT),
            WrenchAction.ofChannel("channel_output", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::outputChannel, DoubleInputTransformerBlockEntity::setOutputChannel)
    );

    public DoubleInputTransformerBlock(Settings settings) {
        super(settings);
    }


    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getPlayerLookDirection().getOpposite();

        Direction.Axis axis;

        if (facing.getAxis() == Direction.Axis.Y) {
            axis = ctx.getHorizontalPlayerFacing().getAxis();
        } else {
            axis = AxisAndFacingBlock.getAxis(facing, false);
        }

        return this.getDefaultState().with(FACING_OUTPUT, facing)
                .with(FACING_INPUT_1, facing.rotateCounterclockwise(axis))
                .with(FACING_INPUT_2, facing.rotateClockwise(axis));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING_INPUT_1, FACING_INPUT_2, FACING_OUTPUT);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DoubleInputTransformerBlockEntity(pos, state);
    }

    @Override
    public boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
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
                && FactoryNodes.DATA.getGraphWorld(world).getNodesAt(selfPos).filter(x -> x.getNode() == node).findFirst().map(NodeHolder::getGraphId)
                        .orElse(-1l).longValue() != FactoryNodes.DATA.getGraphWorld(world).getNodesAt(selfPos).filter(x -> x.getNode() instanceof DataProviderNode)
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

    protected abstract DataContainer transformData(DataContainer input1, DataContainer input2, ServerWorld world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be);

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

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends BlockModel {
        public static final ItemStack INPUT_A = ItemDisplayElementUtil.getModel(id("block/data_cube_connector_input_a"));
        public static final ItemStack INPUT_B = ItemDisplayElementUtil.getModel(id("block/data_cube_connector_input_b"));
        public static final ItemStack OUTPUT = ItemDisplayElementUtil.getModel(id("block/data_cube_connector_output"));
        private final ItemDisplayElement base;
        private final ItemDisplayElement inputA;
        private final ItemDisplayElement inputB;
        private final ItemDisplayElement output;

        private Model(BlockState state) {
            this.base = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            this.inputA = ItemDisplayElementUtil.createSimple(INPUT_A);
            this.inputB = ItemDisplayElementUtil.createSimple(INPUT_B);
            this.output = ItemDisplayElementUtil.createSimple(OUTPUT);

            updateStatePos(state, FACING_INPUT_1, inputA);
            updateStatePos(state, FACING_INPUT_2, inputB);
            updateStatePos(state, FACING_OUTPUT, output);
            this.addElement(this.base);
            this.addElement(this.inputA);
            this.addElement(this.inputB);
            this.addElement(this.output);
        }

        private void updateStatePos(BlockState state, EnumProperty<Direction> property, ItemDisplayElement element) {
            var dir = state.get(property);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.getPositiveHorizontalDegrees();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }

            element.setYaw(y);
            element.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                updateStatePos(state, FACING_INPUT_1, inputA);
                updateStatePos(state, FACING_INPUT_2, inputB);
                updateStatePos(state, FACING_OUTPUT, output);
                this.inputA.tick();
                this.inputB.tick();
                this.output.tick();
            }
        }
    }
}
