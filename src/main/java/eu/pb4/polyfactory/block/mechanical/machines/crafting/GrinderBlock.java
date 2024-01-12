package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.AbovePlacingLimiter;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class GrinderBlock extends RotationalNetworkBlock implements FactoryBlock, BlockEntityProvider, RotationUser, AbovePlacingLimiter {
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
        return List.of(new FunctionalAxisNode(Direction.Axis.Y));
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

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, INPUT_FACING);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.transform(state, mirror::apply, INPUT_FACING);
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

    public static final class Model extends RotationAwareModel {
        public static final ItemStack MODEL_STONE_WHEEL = new ItemStack(BaseItemProvider.requestModel());

        static {
            MODEL_STONE_WHEEL.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(MODEL_STONE_WHEEL.getItem(), FactoryUtil.id("block/grindstone_wheel")).value());
        }

        private final ItemDisplayElement stoneWheel;
        private final ItemDisplayElement main;

        private Model(ServerWorld world, BlockState state) {

            this.main = LodItemDisplayElement.createSimple(FactoryItems.GRINDER);
            this.stoneWheel = LodItemDisplayElement.createSimple(MODEL_STONE_WHEEL, this.getUpdateRate(), 0.6f, 0.6f);

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
            if (this.getTick() % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(),
                        this.blockState().get(INPUT_FACING));
                if (this.stoneWheel.isDirty()) {
                    this.stoneWheel.startInterpolation();
                }
            }
        }
    }
}
