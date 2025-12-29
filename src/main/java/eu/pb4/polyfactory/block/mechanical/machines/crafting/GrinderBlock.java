package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.AbovePlacingLimiter;
import eu.pb4.factorytools.api.block.FactoryBlock;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class GrinderBlock extends RotationalNetworkBlock implements FactoryBlock, EntityBlock, RotationUser, AbovePlacingLimiter {
    public static final Property<Direction> INPUT_FACING = EnumProperty.create("input_facing", Direction.class, x -> x.getAxis() != Direction.Axis.Y);

    public GrinderBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState());
        Model.MODEL_STONE_WHEEL.isEmpty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INPUT_FACING);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(Direction.Axis.Y));
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (!this.canPlaceAbove(null, ctx, ctx.getLevel().getBlockState(ctx.getClickedPos().above()))) {
            return null;
        }

        return this.defaultBlockState().setValue(INPUT_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean canPlaceAbove(@Nullable BlockState self, BlockPlaceContext context, BlockState state) {
        if (state.isAir()) {
            return true;
        }

        if (state.is(FactoryBlockTags.GRINDER_TOP_PLACEABLE)) {
            var x = state.getOptionalValue(BlockStateProperties.AXIS);
            if (x.isPresent()) {
                return x.get() == Direction.Axis.Y;
            }

            var y = state.getOptionalValue(BlockStateProperties.FACING);
            if (y.isPresent()) {
                return y.get() == Direction.DOWN;
            }
            return true;
        }
        return false;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof GrinderBlockEntity be) {
            be.openGui((ServerPlayer) player);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player,  hit);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, INPUT_FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, INPUT_FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrinderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.GRINDER ? GrinderBlockEntity::ticker : null;
    }


    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.BARREL.defaultBlockState();
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof GrinderBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack MODEL_STONE_WHEEL = ItemDisplayElementUtil.getSolidModel(FactoryUtil.id("block/grindstone_wheel"));

        private final ItemDisplayElement stoneWheel;
        private final ItemDisplayElement main;

        private Model(ServerLevel world, BlockState state) {

            this.main = ItemDisplayElementUtil.createSolid(FactoryItems.GRINDER);
            this.stoneWheel = LodItemDisplayElement.createSimple(MODEL_STONE_WHEEL, this.getUpdateRate(), 0.6f, 0.6f);

            this.updateAnimation(0, state.getValue(INPUT_FACING));
            this.addElement(this.main);
            this.addElement(this.stoneWheel);
        }

        private void updateAnimation(float speed, Direction direction) {
            var mat = mat();
            mat.rotate(direction.getRotation().mul(Direction.NORTH.getRotation()));
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
                        this.blockState().getValue(INPUT_FACING));
                this.stoneWheel.startInterpolationIfDirty();
            }
        }
    }
}
