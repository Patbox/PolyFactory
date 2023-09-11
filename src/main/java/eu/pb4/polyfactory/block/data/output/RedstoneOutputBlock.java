package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.block.other.RedstoneConnectable;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
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

public class RedstoneOutputBlock extends GenericDirectionalDataBlock implements DataReceiver, RedstoneConnectable {
    public static final IntProperty POWER = Properties.POWER;

    public RedstoneOutputBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWER, 0));
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection());
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
        return List.of(new ChannelReceiverDirectionNode(state.get(FACING).getOpposite(), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data) {
        var val = MathHelper.clamp(data.asRedstoneOutput(), 0, 15);
        if (selfState.get(POWER) != val) {
            world.setBlockState(selfPos, selfState.with(POWER, val));
        }

        return true;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return state.get(FACING).getOpposite() == dir;
    }

    public static class Model extends BaseModel {
        public static final PolymerModelData OUTPUT_OVERLAY = PolymerResourcePackUtils.requestModel(Items.LEATHER_HELMET, id("block/redstone_output_overlay"));
        public static final PolymerModelData INPUT_OVERLAY = PolymerResourcePackUtils.requestModel(Items.LEATHER_HELMET, id("block/redstone_input_overlay"));
        private final LodItemDisplayElement base;
        private final LodItemDisplayElement overlay;
        public Model(BlockState state) {
            this.base = LodItemDisplayElement.createSimple(state.getBlock().asItem());
            this.overlay = LodItemDisplayElement.createSimple(createOverlay(state));
            //this.overlay.setBrightness(new Brightness(state.get(POWER), 0));
            this.base.setScale(new Vector3f(2));
            this.overlay.setScale(new Vector3f(2));
            this.overlay.setViewRange(0.6f);

            updateStatePos(state);
            this.addElement(this.base);
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

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.asRotation();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }


            this.base.setYaw(y);
            this.base.setPitch(p);
            this.overlay.setYaw(y);
            this.overlay.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = BlockBoundAttachment.get(this).getBlockState();
                updateStatePos(state);
                this.overlay.setItem(createOverlay(state));
                //this.overlay.setBrightness(new Brightness(state.get(POWER), 0));
                this.base.tick();
                this.overlay.tick();
            }
        }
    }
}
