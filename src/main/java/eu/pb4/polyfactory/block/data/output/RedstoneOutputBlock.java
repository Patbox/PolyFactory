package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.RedstoneConnectable;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverSelectiveSideNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class RedstoneOutputBlock extends GenericCabledDataBlock implements DataReceiver, RedstoneConnectable {
    public static final IntProperty POWER = Properties.POWER;

    public RedstoneOutputBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWER, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWER);
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
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelReceiverSelectiveSideNode(getDirections(state), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node) {
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
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
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

    public static class Model extends GenericCabledDataBlock.Model {
        public static final PolymerModelData OUTPUT_OVERLAY = PolymerResourcePackUtils.requestModel(Items.LEATHER_HELMET, id("block/redstone_output_overlay"));
        public static final PolymerModelData INPUT_OVERLAY = PolymerResourcePackUtils.requestModel(Items.LEATHER_HELMET, id("block/redstone_input_overlay"));
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
            var model = state.isOf(FactoryBlocks.REDSTONE_OUTPUT) ? OUTPUT_OVERLAY : INPUT_OVERLAY;
            var stack = new ItemStack(model.item());
            var display = new NbtCompound();
            display.putInt("color", RedstoneWireBlock.getWireColor(state.get(POWER)));
            stack.getOrCreateNbt().put("display", display);
            stack.getNbt().putInt("CustomModelData", model.value());
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
