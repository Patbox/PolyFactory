package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.network.NetworkComponent.RotationalConnector;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.mechanical.AxleWithGearMechanicalNode;
import eu.pb4.polyfactory.nodes.mechanical_connectors.SmallGearNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class AxleWithGearBlock extends AxleBlock implements RotationalConnector, GearPlacementAligner {
    public AxleWithGearBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var otherState = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(ctx.getSide().getOpposite()));
        var axis = ctx.getSide().getAxis();
        if (otherState.getBlock() instanceof GearPlacementAligner gearPlacementAligner
                && gearPlacementAligner.isLargeGear(otherState) == this.isLargeGear(this.getDefaultState())
                && !ctx.shouldCancelInteraction()) {
            axis = gearPlacementAligner.getGearAxis(otherState);
        }

        return waterLog(ctx, this.getDefaultState()).with(AXIS, axis);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (placer instanceof ServerPlayerEntity player) {
            var axis = state.get(AXIS);
            var mut = new BlockPos.Mutable();
            for (var dir : Direction.values()) {
                if (dir.getAxis() != axis) {
                    var state2 = world.getBlockState(mut.set(pos).move(dir).move(dir.rotateClockwise(axis)));

                    if (state2.getBlock() instanceof AxleWithGearBlock && state2.getBlock() != state.getBlock() && state2.get(AxleBlock.AXIS) == axis) {
                        TriggerCriterion.trigger(player, FactoryTriggers.CONNECT_DIFFERENT_GEARS);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState().with(WATERLOGGED, state.get(WATERLOGGED));
    }

    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext context) {
        return false;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return FactoryItems.STEEL_GEAR.getDefaultStack();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new AxleWithGearMechanicalNode(state.get(AXIS)));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState, pos);
    }

    @Override
    public Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SmallGearNode(state.get(AXIS)));
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return super.isSameNetworkType(block) || block instanceof RotationalConnector;
    }

    @Override
    protected void updateNetworkAt(WorldView world, BlockPos pos) {
        super.updateNetworkAt(world, pos);
        RotationalConnector.updateRotationalConnectorAt(world, pos);
    }

    @Override
    public boolean isLargeGear(BlockState state) {
        return false;
    }

    @Override
    public Direction.Axis getGearAxis(BlockState state) {
        return state.get(AXIS);
    }

    @Override
    public boolean placesLikeAxle() {
        return false;
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack ITEM_MODEL_1 = ItemDisplayElementUtil.getModel( id("block/axle_with_gear_1"));
        public static final ItemStack ITEM_MODEL_2 = ItemDisplayElementUtil.getModel( id("block/axle_with_gear_2"));

        private final ItemDisplayElement mainElement;
        private Model(ServerWorld world, BlockState state, BlockPos pos) {
            this.mainElement = LodItemDisplayElement.createSimple(
                    ((pos.getX() + pos.getY() + pos.getZ()) % 2 == 0) ? ITEM_MODEL_2 : ITEM_MODEL_1,
                    this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.updateAnimation(0,  state.get(AXIS));
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.SOUTH.getRotationQuaternion());
            }

            mat.rotateY(rotation);
            mat.scale(2, 2f, 2);

            this.mainElement.setTransformation(mat);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var pos = this.blockAware().getBlockPos();
                this.mainElement.setItem(((pos.getX() + pos.getY() + pos.getZ()) % 2 == 0) ? ITEM_MODEL_2 : ITEM_MODEL_1);
            }
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(),
                        this.blockAware().getBlockState().get(AXIS));
                this.mainElement.startInterpolationIfDirty();
            }
        }
    }
}
