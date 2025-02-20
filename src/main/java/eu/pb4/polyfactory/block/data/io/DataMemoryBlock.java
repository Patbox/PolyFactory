package eu.pb4.polyfactory.block.data.io;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.providers.DataProviderBlock;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.data.util.DataCache;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public final class DataMemoryBlock extends DataProviderBlock implements DataReceiver {
    public static final BooleanProperty READ_ONLY = FactoryProperties.READ_ONLY;
    public static final BooleanProperty POWERED = Properties.POWERED;

    public static final List<WrenchAction> ACTIONS = List.of(
            WrenchAction.CHANNEL,
            WrenchAction.FACING,
            WrenchAction.of("read_only", READ_ONLY, ScreenTexts::onOrOff)
    );

    public DataMemoryBlock(Settings settings) {
        super(settings, false);
        this.setDefaultState(this.getDefaultState().with(READ_ONLY, false).with(POWERED, false));
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx)
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()))
                .with(READ_ONLY, ctx.getStack().getOrDefault(FactoryDataComponents.READ_ONLY, false))
                ;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        var x = super.getPickStack(world, pos, state, includeData);
        x.set(FactoryDataComponents.READ_ONLY, state.get(READ_ONLY));
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
        builder.add(READ_ONLY);
        builder.add(POWERED);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (!world.isClient) {
            boolean bl = state.get(POWERED);
            if (bl != world.isReceivingRedstonePower(pos)) {
                if (bl) {
                    world.scheduleBlockTick(pos, this, 1);
                } else {
                    world.setBlockState(pos, state.with(POWERED, true), 2);
                    if (world.getBlockEntity(pos) instanceof ChanneledDataCache be) {
                        DataProvider.sendData(world, pos, be.getCachedData());
                    }
                }
            }
        }
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(POWERED) && !world.isReceivingRedstonePower(pos)) {
            world.setBlockState(pos, state.with(POWERED, false), 2);
        }
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        var channel = getChannel(world, pos);
        var dir = state.get(FACING);
        return state.get(READ_ONLY)
                ? List.of(new ChannelProviderDirectionNode(dir, channel))
                : List.of(
                new ChannelProviderDirectionNode(dir, channel),
                new ChannelReceiverDirectionNode(dir, channel)
        );
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        if (!selfState.get(READ_ONLY) && world.getBlockEntity(selfPos) instanceof ChanneledDataCache be) {
            be.setCachedData(data);
            return true;
        }

        return false;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return ACTIONS;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends GenericDirectionalDataBlock.Model {
        public static final ItemStack POWERED_MODEL = ItemDisplayElementUtil.getModel(id("block/data_memory_powered"));

        protected Model(BlockState state) {
            super(state, false);
        }

        @Override
        protected void updateStatePos(BlockState state) {
            this.base.setItem(state.get(POWERED) ? POWERED_MODEL : ItemDisplayElementUtil.getModel(state.getBlock().asItem()));
            super.updateStatePos(state);
        }
    }
}
