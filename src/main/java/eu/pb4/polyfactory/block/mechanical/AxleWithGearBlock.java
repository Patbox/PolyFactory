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
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class AxleWithGearBlock extends AxleBlock implements RotationalConnector, GearPlacementAligner {
    public AxleWithGearBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var otherState = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite()));
        var axis = ctx.getClickedFace().getAxis();
        if (otherState.getBlock() instanceof GearPlacementAligner gearPlacementAligner
                && gearPlacementAligner.isLargeGear(otherState) == this.isLargeGear(this.defaultBlockState())
                && !ctx.isSecondaryUseActive()) {
            axis = gearPlacementAligner.getGearAxis(otherState);
        }

        return waterLog(ctx, this.defaultBlockState()).setValue(AXIS, axis);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (placer instanceof ServerPlayer player) {
            var axis = state.getValue(AXIS);
            var mut = new BlockPos.MutableBlockPos();
            for (var dir : Direction.values()) {
                if (dir.getAxis() != axis) {
                    var state2 = world.getBlockState(mut.set(pos).move(dir).move(dir.getClockWise(axis)));

                    if (state2.getBlock() instanceof AxleWithGearBlock && state2.getBlock() != state.getBlock() && state2.getValue(AxleBlock.AXIS) == axis) {
                        TriggerCriterion.trigger(player, FactoryTriggers.CONNECT_DIFFERENT_GEARS);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return FactoryItems.STEEL_GEAR.getDefaultInstance();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new AxleWithGearMechanicalNode(state.getValue(AXIS)));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock.Model(world, initialBlockState, pos);
    }

    @Override
    public Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SmallGearNode(state.getValue(AXIS)));
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return super.isSameNetworkType(block) || block instanceof RotationalConnector;
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        super.updateNetworkAt(world, pos);
        RotationalConnector.updateRotationalConnectorAt(world, pos);
    }

    @Override
    public boolean isLargeGear(BlockState state) {
        return false;
    }

    @Override
    public Direction.Axis getGearAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean placesLikeAxle() {
        return false;
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack ITEM_MODEL_1 = ItemDisplayElementUtil.getModel( id("block/axle_with_gear_1"));
        public static final ItemStack ITEM_MODEL_2 = ItemDisplayElementUtil.getModel( id("block/axle_with_gear_2"));

        private final ItemDisplayElement mainElement;
        private Model(ServerLevel world, BlockState state, BlockPos pos) {
            this.mainElement = LodItemDisplayElement.createSimple(
                    ((pos.getX() + pos.getY() + pos.getZ()) % 2 == 0) ? ITEM_MODEL_2 : ITEM_MODEL_1,
                    this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.updateAnimation(0,  state.getValue(AXIS));
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
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
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(),
                        this.blockAware().getBlockState().getValue(AXIS));
                this.mainElement.startInterpolationIfDirty();
            }
        }
    }
}
