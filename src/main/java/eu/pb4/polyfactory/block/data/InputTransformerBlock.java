package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
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
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
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

public abstract class InputTransformerBlock extends DataNetworkBlock implements BlockEntityProvider, FactoryBlock, CableConnectable, DataProvider, DataReceiver, ConfigurableBlock {
    public static final EnumProperty<Direction> FACING_INPUT = EnumProperty.of("facing_input", Direction.class);
    public static final EnumProperty<Direction> FACING_OUTPUT = EnumProperty.of("facing_output", Direction.class);

     protected static final List<BlockConfig<?>> BLOCK_CONFIG = List.of(
            BlockConfig.ofDirection(FACING_INPUT),
            BlockConfig.ofChannelWithDisabled("channel_input", InputTransformerBlockEntity.class,
                    InputTransformerBlockEntity::inputChannel, InputTransformerBlockEntity::setInputChannel),
            BlockConfig.ofDirection(FACING_OUTPUT),
            BlockConfig.ofChannel("channel_output", InputTransformerBlockEntity.class,
                    InputTransformerBlockEntity::outputChannel, InputTransformerBlockEntity::setOutputChannel)
    );

    public InputTransformerBlock(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getPlayerLookDirection().getOpposite();

        return this.getDefaultState().with(FACING_OUTPUT, facing.getOpposite())
                .with(FACING_INPUT, facing);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING_INPUT, FACING_OUTPUT);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InputTransformerBlockEntity(pos, state);
    }

    @Override
    public boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING_INPUT) == dir
                || state.get(FACING_OUTPUT) == dir;
    }

    @Override
    public @Nullable DataContainer provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof InputTransformerBlockEntity be) {
            return be.lastOutput();
        }

        return null;
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        if (node instanceof ChannelReceiverDirectionNode direction && world.getBlockEntity(selfPos) instanceof InputTransformerBlockEntity be) {
            DataContainer input = be.lastInput();
            boolean matchingData = false;
            if (direction.direction() == selfState.get(FACING_INPUT) && channel == be.inputChannel()) {
                input = be.setLastInput(data);
                matchingData = true;
            }

            if (matchingData) {
                sendData(world, selfState.get(FACING_OUTPUT), selfPos, this.transformData(input, world, selfPos, selfState, be));
                return true;
            }
        }

        return false;
    }

    protected abstract DataContainer transformData(DataContainer input, ServerWorld world, BlockPos selfPos, BlockState selfState, InputTransformerBlockEntity be);

    public int sendData(WorldAccess world, Direction direction, BlockPos selfPos, DataContainer data) {
        if (data != null && world instanceof ServerWorld serverWorld && world.getBlockEntity(selfPos) instanceof InputTransformerBlockEntity be) {
            be.setLastOutput(data);
            return Data.getLogic(serverWorld, selfPos,
                    x -> x.getNode() instanceof ChannelProviderDirectionNode p && p.channel() == be.outputChannel() && p.direction() == direction)
                    .pushDataUpdate(selfPos, be.outputChannel(), data, direction);
        }
        return 0;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof InputTransformerBlockEntity be) {
            return List.of(
                    new ChannelReceiverDirectionNode(state.get(FACING_INPUT), be.inputChannel()),
                    new ChannelProviderDirectionNode(state.get(FACING_OUTPUT), be.outputChannel())
            );
        }
        return List.of();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return BLOCK_CONFIG;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends BlockModel {
        public static final ItemStack INPUT = BaseItemProvider.requestModel(id("block/data_cube_connector_input"));
        public static final ItemStack OUTPUT = BaseItemProvider.requestModel(id("block/data_cube_connector_output"));
        private final ItemDisplayElement base;
        private final ItemDisplayElement input;
        private final ItemDisplayElement output;

        private Model(BlockState state) {
            this.base = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            this.input = ItemDisplayElementUtil.createSimple(INPUT);
            this.output = ItemDisplayElementUtil.createSimple(OUTPUT);

            updateStatePos(state, FACING_INPUT, input);
            updateStatePos(state, FACING_OUTPUT, output);
            this.addElement(this.base);
            this.addElement(this.input);
            this.addElement(this.output);
        }

        private void updateStatePos(BlockState state, EnumProperty<Direction> property, ItemDisplayElement element) {
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
                updateStatePos(state, FACING_INPUT, input);
                updateStatePos(state, FACING_OUTPUT, output);
                this.input.tick();
                this.output.tick();
            }
        }
    }
}
