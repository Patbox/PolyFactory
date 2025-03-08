package eu.pb4.polyfactory.block.mechanical.source;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.ItemUseLimiter;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class HandCrankBlock extends RotationalNetworkBlock implements FactoryBlock, RotationUser, BlockEntityProvider,  BarrierBasedWaterloggable, WrenchableBlock, ItemUseLimiter.All {
    public static final DirectionProperty FACING = Properties.FACING;

    public HandCrankBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getSide().getOpposite()));
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof HandCrankBlockEntity be && be.lastTick != world.getServer().getTicks()) {
            player.addExhaustion(0.1f);
            be.lastTick = world.getServer().getTicks();
            be.negative = player.isSneaking() != (state.get(FACING).getDirection() == Direction.AxisDirection.NEGATIVE);
            var rot = RotationUser.getRotation(world, pos);
            if (player instanceof ServerPlayerEntity serverPlayer && rot.getContext().getGraph().getCachedNodes(FunctionalNode.CACHE).size() > 1) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.POWER_HAND_CRANK);
            }

            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof HandCrankBlockEntity be) {
            var speed = MathHelper.lerp(MathHelper.clamp(((world.getServer().getTicks() - be.lastTick) / 2d - 5) / 5d, 0, 1), RotationConstants.HAND_CRANK_SPEED, 0);
            var stress = MathHelper.lerp(MathHelper.clamp(((world.getServer().getTicks() - be.lastTick) / 2d - 5) / 5d, 0, 1), RotationConstants.HAND_CRANK_STRESS, 0);

            if (speed > 0) {
                modifier.provide(state.get(WATERLOGGED) ? speed / 2 : speed, stress, be.negative);
            }
        }
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
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.get(FACING)));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.STRIPPED_OAK_LOG.getDefaultState();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HandCrankBlockEntity(pos, state);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public List<WrenchAction> getWrenchActions(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(WrenchAction.FACING);
    }

    @Override
    public boolean preventUseItemWhileTargetingBlock(ServerPlayerEntity player, BlockState state, World world, BlockHitResult result, ItemStack stack, Hand hand) {
        return All.super.preventUseItemWhileTargetingBlock(player, state, world, result, stack, hand) && (state.get(WATERLOGGED) ? !stack.isOf(Items.BUCKET) : !stack.isOf(Items.WATER_BUCKET));
    }

    public final class Model extends RotationAwareModel {
        private final ItemDisplayElement mainElement;

        private Model(ServerWorld world, BlockState state) {
            this.mainElement = LodItemDisplayElement.createSimple(FactoryItems.HAND_CRANK, this.getUpdateRate());
            this.updateAnimation(0, state.get(FACING));
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float speed, Direction facing) {
            mat.identity();
            mat.rotate(facing.getOpposite().getRotationQuaternion());
            mat.rotateY(((float) ((facing.getDirection() == Direction.AxisDirection.NEGATIVE) ? speed : -speed)));

            mat.scale(2f);
            this.mainElement.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(),
                        this.blockState().get(FACING));
                if (this.mainElement.isDirty()) {
                    this.mainElement.startInterpolation();
                }
            }
        }
    }
}
