package eu.pb4.polyfactory.block.data.io;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.MapCodec;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.DataCache;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.other.StatePropertiesCodecPatcher;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.util.OptionalDirection;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;

import static eu.pb4.polyfactory.ModInit.id;

public final class DataMemoryBlock extends DataNetworkBlock implements EntityBlock, FactoryBlock, CableConnectable, DataProvider, DataReceiver, ConfigurableBlock, StatePropertiesCodecPatcher {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<OptionalDirection> FACING_INPUT = EnumProperty.create("facing_input", OptionalDirection.class);
    public static final EnumProperty<Direction> FACING_OUTPUT = EnumProperty.create("facing_output", Direction.class);

    public static final List<BlockConfig<?>> ACTIONS = List.of(
            BlockConfig.of("facing_input", FACING_INPUT, (optionalDirection, world, pos, side, state) -> optionalDirection.asText()).withAlt((val, next, player, world, pos, dir, state) -> {
                var direction = OptionalDirection.of(next ? dir : dir.getOpposite());
                return state.getValue(FACING_INPUT) != direction ? direction : OptionalDirection.NONE;
            }),
            BlockConfig.ofChannel("channel_input", DataMemoryBlockEntity.class,
                    DataMemoryBlockEntity::inputChannel, DataMemoryBlockEntity::setInputChannel),
            BlockConfig.ofDirection(FACING_OUTPUT),
            BlockConfig.ofChannel("channel_output", DataMemoryBlockEntity.class,
                    DataMemoryBlockEntity::outputChannel, DataMemoryBlockEntity::setOutputChannel)
    );

    public DataMemoryBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getNearestLookingDirection().getOpposite();

        return this.defaultBlockState().setValue(FACING_OUTPUT, facing.getOpposite())
                .setValue(FACING_INPUT,  ctx.getItemInHand().getOrDefault(FactoryDataComponents.READ_ONLY, false) ? OptionalDirection.NONE : OptionalDirection.of(facing))
                .setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        var x = super.getCloneItemStack(world, pos, state, includeData);
        x.set(FactoryDataComponents.READ_ONLY, state.getValue(FACING_INPUT) == OptionalDirection.NONE);
        if (world.getBlockEntity(pos) instanceof DataCache cache && cache.getCachedData() != null) {
            x.set(FactoryDataComponents.STORED_DATA, cache.getCachedData());
        }

        return x;
    }

    // Todo
    @Override
    public void playerDestroy(Level world, Player player1, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (blockEntity instanceof DataCache cache && cache.getCachedData() != null && !cache.getCachedData().isEmpty()
                && player1 instanceof ServerPlayer player) {
            TriggerCriterion.trigger(player, FactoryTriggers.DATA_MEMORY);
        }
        super.playerDestroy(world, player1, pos, state, blockEntity, tool);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING_INPUT);
        builder.add(FACING_OUTPUT);
        builder.add(POWERED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (!world.isClientSide()) {
            boolean bl = state.getValue(POWERED);
            if (bl != world.hasNeighborSignal(pos)) {
                if (bl) {
                    world.scheduleTick(pos, this, 1);
                } else {
                    world.setBlock(pos, state.setValue(POWERED, true), 2);
                    if (world.getBlockEntity(pos) instanceof DataMemoryBlockEntity be) {
                        DataProvider.sendData(world, pos, be.outputChannel(), be.getCachedData());
                    }
                }
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED) && !world.hasNeighborSignal(pos)) {
            world.setBlock(pos, state.setValue(POWERED, false), 2);
        }
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof DataMemoryBlockEntity be) {
            var input = state.getValue(FACING_INPUT);
            var output = state.getValue(FACING_OUTPUT);
            return input == OptionalDirection.NONE
                    ? List.of(new ChannelProviderDirectionNode(output, be.outputChannel()))
                    : List.of(
                    new ChannelProviderDirectionNode(output, be.outputChannel()),
                    new ChannelReceiverDirectionNode(input.direction(), be.inputChannel())
            );
        }
        return List.of();
    }

    @Override
    public boolean receiveData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir, int dataId) {
        if (node instanceof DirectionNode node1 && selfState.getValue(FACING_INPUT).direction() == node1.direction() && world.getBlockEntity(selfPos) instanceof DataCache be) {
            be.setCachedData(data);
            return true;
        }
        return false;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return ACTIONS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataMemoryBlockEntity(pos, state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(FACING_OUTPUT) == dir || state.getValue(FACING_INPUT).direction() == dir;
    }

    @Override
    public @Nullable DataContainer provideData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof DataMemoryBlockEntity be) {
            return be.getCachedData();
        }
        return null;
    }

    @Override
    public MapCodec<BlockState> modifyPropertiesCodec(MapCodec<BlockState> codec) {
        return StatePropertiesCodecPatcher.modifier(codec, (state, ops, input) -> {
            var facing = input.get("facing");
            var readOnly = input.get("read_only");
            if (facing != null && readOnly != null) {
                try {
                    var dir = Direction.byName(ops.getStringValue(facing).getOrThrow());
                    return state.setValue(FACING_OUTPUT, dir).setValue(FACING_INPUT, ops.getStringValue(readOnly).getOrThrow().equals("true") ? OptionalDirection.NONE : OptionalDirection.of(dir));
                } catch (Throwable ignored) {
                    ignored.printStackTrace();
                }
            }
            return state;
        });
    }

    public static class Model extends BlockModel {
        public static final ItemStack BASE = ItemDisplayElementUtil.getModel(id("block/data_memory"));
        public static final ItemStack COVER = ItemDisplayElementUtil.getModel(id("block/data_memory_cover"));
        public static final ItemStack COVER_POWERED = ItemDisplayElementUtil.getModel(id("block/data_memory_cover_powered"));
        public static final ItemStack INPUT = ItemDisplayElementUtil.getModel(id("block/data_cube_connector_input"));
        public static final ItemStack OUTPUT = ItemDisplayElementUtil.getModel(id("block/data_cube_connector_output"));
        private final ItemDisplayElement base;
        private final ItemDisplayElement input;
        private final ItemDisplayElement output;
        private final ItemDisplayElement[] covers = new ItemDisplayElement[6];
        protected Model(BlockState state) {
            super();
            this.base = ItemDisplayElementUtil.createSimple(BASE);
            this.base.setScale(new Vector3f(2));

            this.input = ItemDisplayElementUtil.createSimple(INPUT);
            this.output = ItemDisplayElementUtil.createSimple(OUTPUT);

            for (var i = 0; i < 6; i++) {
                this.covers[i] = ItemDisplayElementUtil.createSimple(state.getValue(POWERED) ? COVER_POWERED : COVER);
                var dir = Direction.from3DDataValue(i);
                if (dir.getAxis() == Direction.Axis.Y) {
                    this.covers[i].setPitch(dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? -90 : 90);
                    this.covers[i].setYaw(0);
                } else {
                    this.covers[i].setPitch(0);
                    this.covers[i].setYaw(dir.toYRot());
                }
            }

            this.covers[state.getValue(FACING_OUTPUT).get3DDataValue()].setItem(ItemStack.EMPTY);
            updateStatePos(state.getValue(FACING_OUTPUT), output);

            if (state.getValue(FACING_INPUT) == OptionalDirection.NONE) {
                this.input.setItem(ItemStack.EMPTY);
            } else {
                this.covers[state.getValue(FACING_INPUT).direction().get3DDataValue()].setItem(ItemStack.EMPTY);
                updateStatePos(state.getValue(FACING_INPUT).direction(), this.input);
            }

            for (var i = 0; i < 6; i++) {
                this.addElement(this.covers[i]);
            }

            this.addElement(this.base);
            this.addElement(this.input);
            this.addElement(this.output);
        }

        protected void updateStatePos(Direction dir, ItemDisplayElement element) {
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
                var powered = this.blockState().getValue(POWERED) ? COVER_POWERED : COVER;
                for (var i = 0; i < 6; i++) {
                    this.covers[i].setItem(powered);
                }
                updateStatePos(this.blockState().getValue(FACING_OUTPUT), output);
                this.covers[this.blockState().getValue(FACING_OUTPUT).get3DDataValue()].setItem(ItemStack.EMPTY);

                var input = this.blockState().getValue(FACING_INPUT);
                if (input == OptionalDirection.NONE) {
                    this.input.setItem(ItemStack.EMPTY);
                } else {
                    this.covers[input.direction().get3DDataValue()].setItem(ItemStack.EMPTY);
                    this.input.setItem(INPUT);
                    updateStatePos(input.direction(), this.input);
                }
                this.tick();
            }
        }
    }
}
