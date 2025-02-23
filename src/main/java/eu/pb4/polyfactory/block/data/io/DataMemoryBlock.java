package eu.pb4.polyfactory.block.data.io;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.MapCodec;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
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
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchApplyAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
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
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public final class DataMemoryBlock extends DataNetworkBlock implements BlockEntityProvider, FactoryBlock, CableConnectable, DataProvider, DataReceiver, WrenchableBlock, StatePropertiesCodecPatcher {
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final EnumProperty<OptionalDirection> FACING_INPUT = EnumProperty.of("facing_input", OptionalDirection.class);
    public static final EnumProperty<Direction> FACING_OUTPUT = EnumProperty.of("facing_output", Direction.class);

    public static final List<WrenchAction> ACTIONS = List.of(
            WrenchAction.of("facing_input", FACING_INPUT, OptionalDirection::asText).withAlt(WrenchApplyAction.ofState((player, world, pos, dir, state, next) -> {
                var direction = OptionalDirection.of(next ? dir : dir.getOpposite());
                return state.with(FACING_INPUT, state.get(FACING_INPUT) != direction ? direction : OptionalDirection.NONE);
            })),
            WrenchAction.ofChannel("channel_input", DataMemoryBlockEntity.class,
                    DataMemoryBlockEntity::inputChannel, DataMemoryBlockEntity::setInputChannel),
            WrenchAction.ofDirection(FACING_OUTPUT),
            WrenchAction.ofChannel("channel_output", DataMemoryBlockEntity.class,
                    DataMemoryBlockEntity::outputChannel, DataMemoryBlockEntity::setOutputChannel)
    );

    public DataMemoryBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWERED, false));
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getPlayerLookDirection().getOpposite();

        return this.getDefaultState().with(FACING_OUTPUT, facing.getOpposite())
                .with(FACING_INPUT,  ctx.getStack().getOrDefault(FactoryDataComponents.READ_ONLY, false) ? OptionalDirection.NONE : OptionalDirection.of(facing))
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        var x = super.getPickStack(world, pos, state);
        x.set(FactoryDataComponents.READ_ONLY, state.get(FACING_INPUT) == OptionalDirection.NONE);
        if (world.getBlockEntity(pos) instanceof DataCache cache && cache.getCachedData() != null) {
            x.set(FactoryDataComponents.STORED_DATA, cache.getCachedData());
        }

        return x;
    }
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        if (stack.contains(FactoryDataComponents.STORED_DATA)) {

            var decoded = stack.getOrDefault(FactoryDataComponents.STORED_DATA, DataContainer.empty());
            tooltip.add(
                    Texts.bracketed(
                            Text.translatable("block.polyfactory.data_memory.tooltip.stored_data").formatted(Formatting.YELLOW)
                    ).formatted(Formatting.DARK_GRAY)
            );
            tooltip.add(ScreenTexts.space().append(Text.translatable("block.polyfactory.data_memory.tooltip.type",
                    Text.translatable("data_type.polyfactory." + decoded.type().id())).formatted(Formatting.GRAY)));
            tooltip.add(ScreenTexts.space().append(Text.translatable("block.polyfactory.data_memory.tooltip.value", decoded.asString()).formatted(Formatting.GRAY)));
        }


        if (stack.getOrDefault(FactoryDataComponents.READ_ONLY, false)) {
            tooltip.add(
                    Texts.bracketed(
                            Text.translatable("block.polyfactory.data_memory.tooltip.read_only").formatted(Formatting.RED)
                    ).formatted(Formatting.DARK_GRAY));
        }
    }

    @Override
    public void afterBreak(World world, PlayerEntity player1, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (blockEntity instanceof DataCache cache && cache.getCachedData() != null && !cache.getCachedData().isEmpty()
                && player1 instanceof ServerPlayerEntity player) {
            TriggerCriterion.trigger(player, FactoryTriggers.DATA_MEMORY);
        }
        super.afterBreak(world, player1, pos, state, blockEntity, tool);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING_INPUT);
        builder.add(FACING_OUTPUT);
        builder.add(POWERED);
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean bl = state.get(POWERED);
            if (bl != world.isReceivingRedstonePower(pos)) {
                if (bl) {
                    world.scheduleBlockTick(pos, this, 1);
                } else {
                    world.setBlockState(pos, state.with(POWERED, true), 2);
                    if (world.getBlockEntity(pos) instanceof DataMemoryBlockEntity be) {
                        DataProvider.sendData(world, pos, be.outputChannel(), be.getCachedData());
                    }
                }
            }
        }
    }

    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(POWERED) && !world.isReceivingRedstonePower(pos)) {
            world.setBlockState(pos, state.with(POWERED, false), 2);
        }
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof DataMemoryBlockEntity be) {
            var input = state.get(FACING_INPUT);
            var output = state.get(FACING_OUTPUT);
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
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        if (node instanceof DirectionNode node1 && selfState.get(FACING_INPUT).direction() == node1.direction() && world.getBlockEntity(selfPos) instanceof DataCache be) {
            be.setCachedData(data);
            return true;
        }
        return false;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return ACTIONS;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DataMemoryBlockEntity(pos, state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING_OUTPUT) == dir || state.get(FACING_INPUT).direction() == dir;
    }

    @Override
    public @Nullable DataContainer provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
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
                    return state.with(FACING_OUTPUT, dir).with(FACING_INPUT, ops.getStringValue(readOnly).getOrThrow().equals("true") ? OptionalDirection.NONE : OptionalDirection.of(dir));
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
                this.covers[i] = ItemDisplayElementUtil.createSimple(state.get(POWERED) ? COVER_POWERED : COVER);
                var dir = Direction.byId(i);
                if (dir.getAxis() == Direction.Axis.Y) {
                    this.covers[i].setPitch(dir.getDirection() == Direction.AxisDirection.POSITIVE ? -90 : 90);
                    this.covers[i].setYaw(0);
                } else {
                    this.covers[i].setPitch(0);
                    this.covers[i].setYaw(dir.getPositiveHorizontalDegrees());
                }
            }

            this.covers[state.get(FACING_OUTPUT).getId()].setItem(ItemStack.EMPTY);
            updateStatePos(state.get(FACING_OUTPUT), output);

            if (state.get(FACING_INPUT) == OptionalDirection.NONE) {
                this.input.setItem(ItemStack.EMPTY);
            } else {
                this.covers[state.get(FACING_INPUT).direction().getId()].setItem(ItemStack.EMPTY);
                updateStatePos(state.get(FACING_INPUT).direction(), this.input);
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
                var powered = this.blockState().get(POWERED) ? COVER_POWERED : COVER;
                for (var i = 0; i < 6; i++) {
                    this.covers[i].setItem(powered);
                }
                updateStatePos(this.blockState().get(FACING_OUTPUT), output);
                this.covers[this.blockState().get(FACING_OUTPUT).getId()].setItem(ItemStack.EMPTY);

                var input = this.blockState().get(FACING_INPUT);
                if (input == OptionalDirection.NONE) {
                    this.input.setItem(ItemStack.EMPTY);
                } else {
                    this.covers[input.direction().getId()].setItem(ItemStack.EMPTY);
                    this.input.setItem(INPUT);
                    updateStatePos(input.direction(), this.input);
                }
                this.tick();
            }
        }
    }
}
