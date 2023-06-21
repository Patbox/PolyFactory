package eu.pb4.polyfactory.block.machines;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.AbovePlacingLimiter;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.network.RotationalNetworkBlock;
import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.display.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.mechanical.AxisMechanicalNode;
import eu.pb4.polyfactory.nodes.mechanical.AxisRotationUserNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class GrinderBlock extends RotationalNetworkBlock implements PolymerBlock, BlockEntityProvider, RotationUser, BlockWithElementHolder, AbovePlacingLimiter, VirtualDestroyStage.Marker {
    public static final Property<Direction> INPUT_FACING = DirectionProperty.of("input_facing", x -> x.getAxis() != Direction.Axis.Y);

    public GrinderBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState());
        Model.MODEL_STONE_WHEEL.isEmpty();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(INPUT_FACING);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new AxisRotationUserNode(Direction.Axis.Y));
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }


    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (!this.canPlaceAbove(null, ctx, ctx.getWorld().getBlockState(ctx.getBlockPos().up()))) {
            return null;
        }

        return this.getDefaultState().with(INPUT_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public boolean canPlaceAbove(@Nullable BlockState self, ItemPlacementContext context, BlockState state) {
        if (state.isAir()) {
            return true;
        }

        if (state.isIn(FactoryBlockTags.GRINDER_TOP_PLACEABLE)) {
            var x = state.getOrEmpty(Properties.AXIS);
            if (x.isPresent()) {
                return x.get() == Direction.Axis.Y;
            }

            var y = state.getOrEmpty(Properties.FACING);
            if (y.isPresent()) {
                return y.get() == Direction.DOWN;
            }
            return true;
        }
        return false;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.MAIN_HAND && !player.isSneaking() && world.getBlockEntity(pos) instanceof GrinderBlockEntity be) {
            be.openGui((ServerPlayerEntity) player);
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GrinderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.GRINDER ? GrinderBlockEntity::ticker : null;
    }


    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.GRINDSTONE.getDefaultState();
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof GrinderBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public final class Model extends LodElementHolder {
        public static final ItemStack MODEL_STONE_WHEEL = new ItemStack(Items.CANDLE);

        static {
            MODEL_STONE_WHEEL.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(MODEL_STONE_WHEEL.getItem(), FactoryUtil.id("block/grindstone_wheel")).value());
        }

        private final Matrix4f mat = new Matrix4f();
        private final ItemDisplayElement stoneWheel;
        private final ItemDisplayElement main;

        private Model(ServerWorld world, BlockState state) {
            this.main = new LodItemDisplayElement(FactoryItems.GRINDER_BLOCK.getDefaultStack());
            this.main.setDisplaySize(1, 1);
            this.main.setModelTransformation(ModelTransformationMode.FIXED);
            this.stoneWheel = new LodItemDisplayElement(MODEL_STONE_WHEEL);
            this.stoneWheel.setDisplaySize(1, 1);
            this.stoneWheel.setModelTransformation(ModelTransformationMode.FIXED);
            this.stoneWheel.setInterpolationDuration(5);
            this.updateAnimation(0, state.get(INPUT_FACING));
            this.addElement(this.main);
            this.addElement(this.stoneWheel);
        }

        private void updateAnimation(float speed, Direction direction) {
            mat.identity();
            mat.rotate(direction.getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion()));
            mat.scale(2f);

            this.main.setTransformation(mat);

            mat.rotateY(((float) speed));
            mat.translate(0, 0.5f, 0);
            mat.translate(0, -0.19f, 0);
            this.stoneWheel.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % 4 == 0) {
                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), BlockBoundAttachment.get(this).getBlockPos()).rotation(),
                        ((BlockBoundAttachment) this.getAttachment()).getBlockState().get(INPUT_FACING));
                if (this.stoneWheel.isDirty()) {
                    this.stoneWheel.startInterpolation();
                }
            }
        }
    }
}
