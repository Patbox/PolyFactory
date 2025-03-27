package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.RedstoneConnectable;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ValueFormatter;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverSelectiveSideNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.block.OrientationHelper;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.util.FactoryUtil.id;
import static eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras.bridgeModel;

public class RedstoneOutputBlock extends GenericCabledDataBlock implements DataReceiver, RedstoneConnectable {
    public static final IntProperty POWER = Properties.POWER;
    public static final BooleanProperty STRONG = BooleanProperty.of("strong");

    public final List<BlockConfig<?>> blockConfigs = List.of(
            BlockConfig.CHANNEL,
            this.facingAction,
            BlockConfig.of("redstone.strong", STRONG, ValueFormatter.text(ScreenTexts::onOrOff))
    );

    public RedstoneOutputBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWER, 0).with(STRONG, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWER, STRONG);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        this.update(world, pos, state.get(FACING));
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!moved) {
            this.update(world, pos, state.get(FACING));
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    private void update(World world, BlockPos pos, Direction dir) {
        var wireOrientation = OrientationHelper.getEmissionOrientation(world, null, dir);

        for(var direction : Direction.values()) {
            world.updateNeighborsAlways(pos.offset(direction), this, OrientationHelper.withFrontNullable(wireOrientation, direction));
        }
    }


    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return direction.getOpposite() == state.get(FACING) ? state.get(POWER) : 0;
    }

    @Override
    protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(STRONG) ? getWeakRedstonePower(state, world, pos, direction) : 0;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelReceiverSelectiveSideNode(getDirections(state), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        var val = MathHelper.clamp(data.asRedstoneOutput(), 0, 15);
        if (selfState.get(POWER) != val) {
            world.setBlockState(selfPos, selfState.with(POWER, val));
            if (FactoryUtil.getClosestPlayer(world, selfPos, 32) instanceof ServerPlayerEntity player) {
                TriggerCriterion.trigger(player, FactoryTriggers.REDSTONE_OUT);
            }
        }

        return true;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return state.get(FACING).getOpposite() == dir;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return this.blockConfigs;
    }

    public static class Model extends GenericCabledDataBlock.Model {
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
            var stack = new ItemStack(Items.PAPER);
            stack.set(DataComponentTypes.ITEM_MODEL, state.isOf(FactoryBlocks.REDSTONE_OUTPUT) ? OUTPUT_OVERLAY : INPUT_OVERLAY);
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(), IntList.of(RedstoneWireBlock.getWireColor(state.get(POWER)))));
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
