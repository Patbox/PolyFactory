package eu.pb4.polyfactory.block.mechanical.machines;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.EightWayDirection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class PlanterBlock extends RotationalNetworkBlock implements FactoryBlock, BlockEntityProvider, RotationUser, BarrierBasedWaterloggable {
    public PlanterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState());
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(Direction.Axis.Y));
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof PlanterBlockEntity be ? ScreenHandler.calculateComparatorOutput((Inventory) be) : 0;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (placer instanceof PlayerEntity player && world.getBlockEntity(pos) instanceof PlanterBlockEntity be) {
            be.owner = player.getGameProfile();
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.MAIN_HAND && !player.isSneaking() && world.getBlockEntity(pos) instanceof PlanterBlockEntity be) {
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
            }
            world.updateComparators(pos, this);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PlanterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.PLANTER ? PlanterBlockEntity::ticker : null;
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
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof PlanterBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public final class Model extends RotationAwareModel {
        public static final ItemStack OUTPUT_1 = BaseItemProvider.requestModel(id("block/planter_output"));
        public static final ItemStack OUTPUT_2 = BaseItemProvider.requestModel(id("block/planter_output_2"));

        private final ItemDisplayElement output1;
        private final ItemDisplayElement output2;
        private final ItemDisplayElement main;
        EightWayDirection direction = null;

        private Model(ServerWorld world, BlockState state) {
            this.main = LodItemDisplayElement.createSimple(FactoryItems.PLANTER);
            this.output1 = LodItemDisplayElement.createSimple(OUTPUT_1, 5, 0.3f);
            this.output2 = LodItemDisplayElement.createSimple(OUTPUT_2, 5, 0.3f);
            this.output1.setViewRange(0.5f);
            this.output2.setViewRange(0.5f);

            this.updateAnimation();
            this.addElement(this.main);
            this.addElement(this.output1);
            this.addElement(this.output2);
        }

        private void updateAnimation() {
            mat.identity();
            mat.scale(2f);
            this.main.setTransformation(mat);

            mat.rotateY(MathHelper.HALF_PI);
            mat.scale(0.5f);


            /*if (this.item.getItem().getItem() instanceof ToolItem) {
                mat.translate(-0.1f, 0.25f, 0);
                mat.translate(-0.25f, -0.25f, 0);
                mat.rotateZ(this.rotation);
                mat.translate(0.25f, 0.25f, 0);
            } else {
                mat.translate(-0.3f, 0, 0);
                mat.rotateZ(this.rotation);
            }

            this.item.setTransformation(mat);*/
        }

        @Override
        protected void onTick() {
            //this.updateAnimation(this.blockState().get(FACING));
            if (this.output1.isDirty()) {
                this.output1.startInterpolation();
                this.output2.startInterpolation();
            }
        }

        public void setDirection(EightWayDirection direction) {
            if (this.direction == direction) {
                return;
            }
            this.direction = direction;
            this.output1.setTranslation(new Vector3f(direction.getOffsetX(), 0, direction.getOffsetZ()).mul(3.016f / 16));
            this.output2.setTranslation(new Vector3f(direction.getOffsetX(), 0, direction.getOffsetZ()).mul(3.016f / 16).add(0, 1 / 32f,0));

            this.output2.setLeftRotation(new Quaternionf().rotateY(-MathHelper.HALF_PI / 2 * direction.ordinal()));
        }
    }
}
