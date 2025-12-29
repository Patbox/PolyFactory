package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.RedstoneConnectable;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.BlockValueFormatter;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverSelectiveSideNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;

import static eu.pb4.polyfactory.util.FactoryUtil.id;
import static eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras.bridgeModel;

public class RedstoneOutputBlock extends DirectionalCabledDataBlock implements DataReceiver, RedstoneConnectable {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty STRONG = BooleanProperty.create("strong");

    public final List<BlockConfig<?>> blockConfigs = List.of(
            BlockConfig.CHANNEL,
            this.facingAction,
            BlockConfig.of("redstone.strong", STRONG, BlockValueFormatter.text(CommonComponents::optionStatus))
    );

    public RedstoneOutputBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(POWER, 0).setValue(STRONG, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER, STRONG);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        this.update(world, pos, state.getValue(FACING));
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (!moved) {
            this.update(world, pos, state.getValue(FACING));
        }
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    private void update(Level world, BlockPos pos, Direction dir) {
        var wireOrientation = ExperimentalRedstoneUtils.initialOrientation(world, null, dir);

        for(var direction : Direction.values()) {
            world.updateNeighborsAt(pos.relative(direction), this, ExperimentalRedstoneUtils.withFront(wireOrientation, direction));
        }
    }


    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return direction.getOpposite() == state.getValue(FACING) ? state.getValue(POWER) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(STRONG) ? getSignal(state, world, pos, direction) : 0;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new ChannelReceiverSelectiveSideNode(getDirections(state), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir, int dataId) {
        var val = Mth.clamp(data.asRedstoneOutput(), 0, 15);
        if (selfState.getValue(POWER) != val) {
            world.setBlockAndUpdate(selfPos, selfState.setValue(POWER, val));
            if (FactoryUtil.getClosestPlayer(world, selfPos, 32) instanceof ServerPlayer player) {
                TriggerCriterion.trigger(player, FactoryTriggers.REDSTONE_OUT);
            }
        }

        return true;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.data.output.RedstoneOutputBlock.Model(initialBlockState);
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return state.getValue(FACING).getOpposite() == dir;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return this.blockConfigs;
    }

    public static class Model extends DirectionalCabledDataBlock.Model {
        public static final Identifier OUTPUT_OVERLAY = bridgeModel(id("block/redstone_output_overlay"));
        public static final Identifier INPUT_OVERLAY =  bridgeModel(id("block/redstone_input_overlay"));
        private final ItemDisplayElement overlay;

        public Model(BlockState state) {
            super(state);
            this.overlay = ItemDisplayElementUtil.createSimple(createOverlay(state));
            this.overlay.setScale(new Vector3f(2.005f));
            this.overlay.setViewRange(0.6f);

            updateStatePos(state);
            this.addElement(this.overlay);
        }

        private ItemStack createOverlay(BlockState state) {
            var stack = new ItemStack(Items.STONE);
            stack.set(DataComponents.ITEM_MODEL, state.is(FactoryBlocks.REDSTONE_OUTPUT) ? OUTPUT_OVERLAY : INPUT_OVERLAY);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of(RedStoneWireBlock.getColorForPower(state.getValue(POWER)))));
            return stack;
        }

        @Override
        protected void updateStatePos(BlockState state) {
            super.updateStatePos(state);
            if (this.overlay != null) {
                this.overlay.setYaw(this.base.getYaw());
                this.overlay.setPitch(this.base.getPitch());
            }
        }

        @Override
        protected void setState(BlockState blockState) {
            super.setState(blockState);
            this.overlay.setItem(createOverlay(blockState));
            this.overlay.tick();
        }
    }
}
