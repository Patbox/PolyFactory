package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public abstract class DoubleInputTransformerBlock extends DataNetworkBlock implements BlockEntityProvider, FactoryBlock, CableConnectable, DataProvider, DataReceiver, ConfigurableBlock {
    public static final DirectionProperty FACING_INPUT_1 = DirectionProperty.of("facing_input_1");
    public static final DirectionProperty FACING_INPUT_2 = DirectionProperty.of("facing_input_2");
    public static final DirectionProperty FACING_OUTPUT = DirectionProperty.of("facing_output");

     protected static final List<BlockConfig<?>> WRENCH_ACTIONS = List.of(
            BlockConfig.ofDirection(FACING_INPUT_1),
            BlockConfig.ofChannelWithDisabled("channel_input_1", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::inputChannel1, DoubleInputTransformerBlockEntity::setInputChannel1),

            BlockConfig.ofDirection(FACING_INPUT_2),
            BlockConfig.ofChannelWithDisabled("channel_input_2", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::inputChannel2, DoubleInputTransformerBlockEntity::setInputChannel2),

            BlockConfig.ofDirection(FACING_OUTPUT),
            BlockConfig.ofChannel("channel_output", DoubleInputTransformerBlockEntity.class,
                    DoubleInputTransformerBlockEntity::outputChannel, DoubleInputTransformerBlockEntity::setOutputChannel)
    );

    public DoubleInputTransformerBlock(Settings settings) {
        super(settings);
    }


    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getPlayerLookDirection();

        Direction facing2;

        if (facing.getAxis() == Direction.Axis.Y) {
            facing2 = ctx.getHorizontalPlayerFacing();
        } else {
            facing2 = facing.rotateYClockwise();
            for (var x : ctx.getPlacementDirections()) {
                if (x != facing && x.getAxis() != Direction.Axis.Y) {
                    facing2 = x;
                    break;
                }
            }
        }



        return this.getDefaultState().with(FACING_OUTPUT, facing)
                .with(FACING_INPUT_1, facing.getOpposite())
                .with(FACING_INPUT_2, facing2);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING_INPUT_1, FACING_INPUT_2, FACING_OUTPUT);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
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
        if (node instanceof ChannelReceiverDirectionNode direction && world.getBlockEntity(selfPos) instanceof DoubleInputTransformerBlockEntity be) {
            DataContainer input1 = be.lastInput1();
            DataContainer input2 = be.lastInput2();
            boolean matchingData = false;
            if (direction.direction() == selfState.get(FACING_INPUT_1) && channel == be.inputChannel1()) {
                input1 = be.setLastInput1(data);
                matchingData = true;
            }
            if (direction.direction() == selfState.get(FACING_INPUT_2) && channel == be.inputChannel2()) {
                input2 = be.setLastInput2(data);
                matchingData = true;
            }

            if (matchingData) {
                sendData(world, selfState.get(FACING_OUTPUT), selfPos, this.transformData(input1, input2, world, selfPos, selfState, be));
                return true;
            }
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
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends BlockModel {
        public static final ItemStack INPUT_A = BaseItemProvider.requestModel(id("block/data_cube_connector_input_a"));
        public static final ItemStack INPUT_B = BaseItemProvider.requestModel(id("block/data_cube_connector_input_b"));
        public static final ItemStack OUTPUT = BaseItemProvider.requestModel(id("block/data_cube_connector_output"));
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

        private void updateStatePos(BlockState state, DirectionProperty property, ItemDisplayElement element) {
            var dir = state.get(property);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.asRotation();
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
