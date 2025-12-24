package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import static eu.pb4.polyfactory.ModInit.id;

public abstract class DoubleInputTransformerBlock extends DataNetworkBlock implements EntityBlock, FactoryBlock, CableConnectable, DataProvider, DataReceiver, ConfigurableBlock {
    public static final EnumProperty<Direction> FACING_INPUT_1 = EnumProperty.create("facing_input_1", Direction.class);
    public static final EnumProperty<Direction> FACING_INPUT_2 = EnumProperty.create("facing_input_2", Direction.class);
    public static final EnumProperty<Direction> FACING_OUTPUT = EnumProperty.create("facing_output", Direction.class);

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

    public DoubleInputTransformerBlock(Properties settings) {
        super(settings);
    }


    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getNearestLookingDirection();

        Direction facing2;

        if (facing.getAxis() == Direction.Axis.Y) {
            facing2 = ctx.getHorizontalDirection();
        } else {
            facing2 = facing.getClockWise();
            for (var x : ctx.getNearestLookingDirections()) {
                if (x != facing && x.getAxis() != Direction.Axis.Y) {
                    facing2 = x;
                    break;
                }
            }
        }



        return this.defaultBlockState().setValue(FACING_OUTPUT, facing)
                .setValue(FACING_INPUT_1, facing.getOpposite())
                .setValue(FACING_INPUT_2, facing2);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING_INPUT_1, FACING_INPUT_2, FACING_OUTPUT);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoubleInputTransformerBlockEntity(pos, state);
    }

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(FACING_INPUT_1) == dir
                || state.getValue(FACING_INPUT_2) == dir
                || state.getValue(FACING_OUTPUT) == dir;
    }

    @Override
    public @Nullable DataContainer provideData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof DoubleInputTransformerBlockEntity be
                && node instanceof ChannelProviderDirectionNode providerDirectionNode && providerDirectionNode.direction() == selfState.getValue(FACING_OUTPUT)
                && be.outputChannel() == channel
        ) {
            return be.lastOutput();
        }

        return null;
    }

    @Override
    public boolean receiveData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir, int dataId) {
        if (node instanceof ChannelReceiverDirectionNode direction && world.getBlockEntity(selfPos) instanceof DoubleInputTransformerBlockEntity be) {
            DataContainer input1 = be.lastInput1();
            DataContainer input2 = be.lastInput2();
            boolean matchingData = false;
            if (direction.direction() == selfState.getValue(FACING_INPUT_1) && channel == be.inputChannel1()) {
                input1 = be.setLastInput1(data);
                matchingData = true;
            }
            if (direction.direction() == selfState.getValue(FACING_INPUT_2) && channel == be.inputChannel2()) {
                input2 = be.setLastInput2(data);
                matchingData = true;
            }

            if (matchingData) {
                sendData(world, selfState.getValue(FACING_OUTPUT), selfPos, this.transformData(input1, input2, world, selfPos, selfState, be), dataId);
                return true;
            }
        }

        return false;
    }

    protected abstract DataContainer transformData(DataContainer input1, DataContainer input2, ServerLevel world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be);

    public int sendData(LevelAccessor world, Direction direction, BlockPos selfPos, DataContainer data, int dataId) {
        if (data != null && world instanceof ServerLevel serverWorld && world.getBlockEntity(selfPos) instanceof DoubleInputTransformerBlockEntity be) {
            be.setLastOutput(data);
            return NetworkComponent.Data.getLogic(serverWorld, selfPos,
                    x -> x.getNode() instanceof ChannelProviderDirectionNode p && p.channel() == be.outputChannel() && p.direction() == direction)
                    .pushDataUpdate(selfPos, be.outputChannel(), data, direction, dataId);
        }
        return 0;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            return List.of(
                    new ChannelReceiverDirectionNode(state.getValue(FACING_INPUT_1), be.inputChannel1()),
                    new ChannelReceiverDirectionNode(state.getValue(FACING_INPUT_2), be.inputChannel2()),
                    new ChannelProviderDirectionNode(state.getValue(FACING_OUTPUT), be.outputChannel())
            );
        }
        return List.of();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
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
            var dir = state.getValue(property);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
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
