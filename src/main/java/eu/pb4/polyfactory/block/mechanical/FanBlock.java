package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.generic.SimpleDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class FanBlock extends RotationalNetworkBlock implements FactoryBlock, RotationUser, BlockEntityProvider, WrenchableBlock {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty ENABLED = Properties.ENABLED;
    public static final BooleanProperty REVERSE = BooleanProperty.of("reverse");

    public static final WrenchAction REVERSE_ACTION = WrenchAction.of("reverse", REVERSE);

    public FanBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ENABLED, true).with(REVERSE, false));
        Model.ITEM_MODEL.getItem();
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection());
    }

    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
        super.onBlockAdded(state,world,pos, oldState, notify);
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    private void updateEnabled(World world, BlockPos pos, BlockState state) {
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered == state.get(ENABLED)) {
            world.setBlockState(pos, state.with(ENABLED, !powered), 4);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED, REVERSE);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.get(FACING).getOpposite()));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.FAN ? FanBlockEntity::tick : null;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.transform(state, mirror::apply, FACING);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.SMOOTH_STONE.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(REVERSE_ACTION, WrenchAction.FACING);
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        modifier.stress(0.2);
    }

    public final class Model extends RotationAwareModel {
        public static final ItemStack ITEM_MODEL = new ItemStack(BaseItemProvider.requestModel());

        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement fan;
        private float rotation = 0;
        private boolean reverse;

        private Model(BlockState state) {
            this.mainElement = new LodItemDisplayElement(FactoryItems.FAN.getDefaultStack());
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setScale(new Vector3f(2f));
            this.mainElement.setViewRange(0.8f);
            this.mainElement.setInvisible(true);

            this.fan = LodItemDisplayElement.createSimple(ITEM_MODEL, 2, 0.2f, 0.4f);
            this.fan.setViewRange(0.3f);
            this.updateStatePos(state);
            this.updateAnimation(0);

            this.addElement(this.mainElement);
            this.addElement(this.fan);

        }

        private void updateStatePos(BlockState state) {
            this.reverse = state.get(REVERSE);
            var dir = state.get(FACING);
            float p = 0;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 90;
                y = dir.asRotation();
            } else if (dir == Direction.DOWN) {
                p = 180;
            }


            this.mainElement.setYaw(y);
            this.mainElement.setPitch(p);
            this.fan.setYaw(y);
            this.fan.setPitch(p);
        }

        private void updateAnimation(double speed) {
            this.rotation += ((float) Math.min((this.reverse ? -speed : speed) * MathHelper.RADIANS_PER_DEGREE * 3, RotationConstants.MAX_ROTATION_PER_TICK_2)) % MathHelper.TAU;

            mat.identity();
            mat.rotateY(this.rotation);
            mat.scale(2);
            this.fan.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            if (this.getTick() % 2 == 0) {
                this.updateAnimation(this.getRotationData().speed());
                if (this.fan.isDirty()) {
                    this.fan.startInterpolation();
                }
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        static {
            ITEM_MODEL.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(ITEM_MODEL.getItem(), id("block/fan_rotating")).value());
        }
    }
}
