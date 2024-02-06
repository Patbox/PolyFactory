package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.RedstoneConnectable;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.ItemStackData;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public class HologramProjectorBlock extends GenericDirectionalDataBlock implements DataReceiver, RedstoneConnectable, BarrierBasedWaterloggable {
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    public HologramProjectorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false).with(ACTIVE, false));
        //noinspection ResultOfMethodCallIgnored
        Model.ACTIVE_MODEL.getCount();
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getSide()));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WATERLOGGED).add(ACTIVE);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelReceiverDirectionNode(state.get(FACING).getOpposite(), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data) {
        if (world.getBlockEntity(selfPos) instanceof ChanneledDataBlockEntity be) {
            be.setCachedData(data);
            var active = selfState.get(ACTIVE);
            if (data.isEmpty() == active) {
                world.setBlockState(selfPos, selfState.cycle(ACTIVE));
            }
            return true;
        }
        return false;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return state.get(FACING).getOpposite() == dir;
    }

    public static class Model extends BlockModel implements ChanneledDataBlockEntity.ModelInitializer {
        private static final ItemStack ACTIVE_MODEL = BaseItemProvider.requestModel(id("block/hologram_projector_active"));
        private final LodItemDisplayElement base;
        private DisplayElement currentDisplay;
        private Direction facing;

        public Model(BlockState state) {
            this.base = LodItemDisplayElement.createSimple(state.get(ACTIVE) ? ACTIVE_MODEL : LodItemDisplayElement.getModel(state.getBlock().asItem()));
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.addElement(this.base);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            this.facing = dir;
            float p = 0;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                y = dir.asRotation();
                p = 90;
            } else if (dir == Direction.DOWN) {
                p = 180;
            }


            this.base.setYaw(y);
            this.base.setPitch(p);
            if (this.currentDisplay != null) {
                applyPositionTransformation(this.currentDisplay, this.facing);
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                this.base.setItem(state.get(ACTIVE) ? ACTIVE_MODEL : LodItemDisplayElement.getModel(state.getBlock().asItem()));
                updateStatePos(state);
                this.base.tick();
            }
        }

        @Override
        public void setData(DataContainer data) {
            if (data instanceof ItemStackData stackData) {
                if (this.currentDisplay instanceof ItemDisplayElement e) {
                    e.setItem(stackData.stack());
                    e.tick();
                    return;
                } else if (this.currentDisplay != null) {
                    this.removeElement(this.currentDisplay);
                }
                this.currentDisplay = LodItemDisplayElement.createSimple(stackData.stack(), 4, 0.6f);
                this.applyInitialTransformation(this.currentDisplay);
                this.addElement(this.currentDisplay);
            } else if (this.currentDisplay != null) {
                this.removeElement(this.currentDisplay);
                this.currentDisplay = null;
            }
        }

        private void applyInitialTransformation(DisplayElement display) {
            display.setDisplaySize(16, 16);
            display.setInterpolationDuration(4);
            display.setBrightness(new Brightness(15, 15));
            display.setScale(new Vector3f(3, 3, 0.001f));
            if (display instanceof ItemDisplayElement item) {
                item.setModelTransformation(ModelTransformationMode.FIXED);
            }
            applyPositionTransformation(display, this.facing);
            applyDynamicTransformation(display);
        }

        private void applyDynamicTransformation(DisplayElement display) {
            mat.identity();
            if (display instanceof BlockDisplayElement) {
                mat.translate(-0.5f, -0.5f, -0.5f);
            }
        }

        private void applyPositionTransformation(DisplayElement display, Direction facing) {
            var vec = new Vector3f(0, 2, 0).rotate(facing.getRotationQuaternion());
            display.setOffset(new Vec3d(vec));
        }
    }
}
