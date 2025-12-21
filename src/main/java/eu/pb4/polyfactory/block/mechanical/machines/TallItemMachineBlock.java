package eu.pb4.polyfactory.block.mechanical.machines;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.MovingItemProvider;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public abstract class TallItemMachineBlock extends RotationalNetworkBlock implements FactoryBlock, EntityBlock, WorldlyContainerHolder, RotationUser, MovingItemConsumer, MovingItemProvider {
    public static final Property<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty HAS_CONVEYOR = BooleanProperty.create("has_conveyor");
    public static final Property<Direction> INPUT_FACING = EnumProperty.create("input_facing", Direction.class, x -> x.getAxis() != Direction.Axis.Y);

    public TallItemMachineBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(PART, Part.MAIN).setValue(HAS_CONVEYOR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, INPUT_FACING, HAS_CONVEYOR);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return state.getValue(PART) == Part.TOP ? List.of(new FunctionalAxisNode(state.getValue(INPUT_FACING).getClockWise().getAxis())) : List.of();
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (
                !ctx.getLevel().getBlockState(ctx.getClickedPos().above()).canBeReplaced(BlockPlaceContext.at(ctx, ctx.getClickedPos().above(), Direction.DOWN))
        ) {
            return null;
        }

        return this.defaultBlockState().setValue(INPUT_FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(HAS_CONVEYOR, ctx.getLevel().getBlockState(ctx.getClickedPos().relative(ctx.getHorizontalDirection())).is(FactoryBlocks.CONVEYOR));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide()) {
            BlockPos blockPos = pos.above();
            world.setBlock(blockPos, state.setValue(PART, Part.TOP), 3);
            world.updateNeighborsAt(pos, Blocks.AIR);
            state.updateNeighbourShapes(world, pos, 3);
        }
    }


    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        var part = state.getValue(PART);

        if (((part == Part.MAIN && direction == Direction.UP)
                || (part == Part.TOP && direction == Direction.DOWN)
        ) && !neighborState.is(this)) {
            Rotational.updateRotationalAt(world, pos);
            return Blocks.AIR.defaultBlockState();
        } else if (direction == state.getValue(INPUT_FACING).getOpposite()) {
            return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random).setValue(HAS_CONVEYOR, neighborState.is(FactoryBlocks.CONVEYOR));
        } else {
            return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && !player.isShiftKeyDown()) {
            pos = state.getValue(PART) == Part.MAIN ? pos : pos.below();

            if (world.getBlockEntity(pos) instanceof TallItemMachineBlockEntity be) {
                return be.onUse(state, world, pos, player, hit);
            }
            return InteractionResult.FAIL;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
        if (state.getValue(PART) == Part.MAIN) {
            world.updateNeighbourForOutputSignal(pos, this);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.MAIN ? createSourceBlockEntity(pos, state) : null;
    }

    protected abstract BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state);

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor world, BlockPos pos) {
        pos = switch (state.getValue(PART)) {
            case MAIN -> pos;
            case TOP -> pos.below();
        };

        var be = world.getBlockEntity(pos);

        return be instanceof WorldlyContainer sidedInventory ? sidedInventory : null;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, INPUT_FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, INPUT_FACING);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return initialBlockState.getValue(PART) == Part.MAIN ? createModel(world, pos, initialBlockState) : null;
    }

    protected abstract ElementHolder createModel(ServerLevel serverWorld, BlockPos pos, BlockState initialBlockState);

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(state.getValue(PART) == Part.MAIN ? pos : pos.below()) instanceof TallItemMachineBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public enum Part implements StringRepresentable {
        MAIN,
        TOP;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
