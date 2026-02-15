package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.AbstractFilterItem;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.FilterIcon;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;


public class SplitterBlock extends Block implements FactoryBlock, MovingItemConsumer, ConfigurableBlock, EntityBlock, BarrierBasedWaterloggable {
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty DISTRIBUTE = BooleanProperty.create("distribute");
    public static final BooleanProperty BLOCKING = BooleanProperty.create("blocking");
    public static EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SplitterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(ENABLED, true).setValue(DISTRIBUTE, true).setValue(BLOCKING, true));
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED, DISTRIBUTE, BLOCKING);
        builder.add(WATERLOGGED);

    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        var selfState = self.getBlockState();
        var dir = selfState.getValue(FACING);
        var item = conveyor.getContainer();

        if (!selfState.getValue(ENABLED) || item == null || item.get().isEmpty() || conveyor.movementDelta() < 0.49) {
            return false;
        }
        if (self.getBlockEntity() instanceof SplitterBlockEntity blockEntity) {
            List<Direction> dirs;

            var stack = item.get();

            if (blockEntity.filtersEmpty()) {
                dirs = List.of(dir.getCounterClockWise(), dir, dir.getClockWise());
            } else if (blockEntity.matchesSides(stack)) {
                dirs = List.of(dir.getCounterClockWise(), dir.getClockWise());
            } else if (blockEntity.matchesLeft(item.get())) {
                dirs = List.of(dir.getCounterClockWise());
            } else if (blockEntity.matchesRight(item.get())) {
                dirs = List.of(dir.getClockWise());
            } else if (blockEntity.isLeftFilterEmpty()) {
                dirs = List.of(dir.getCounterClockWise(), dir);
            } else if (blockEntity.isRightFilterEmpty()) {
                dirs = List.of(dir.getClockWise(), dir);
            } else {
                return false;
            }

            if (selfState.getValue(DISTRIBUTE) && dirs.size() > 1) {
                dirs = List.of(dirs.get(blockEntity.pos(dirs.size())));
            }

            for (var direction : dirs) {
                var x = FactoryUtil.tryInsertingMovable(conveyor, self.getWorld(), conveyorPos, conveyorPos.relative(direction), direction, direction, FactoryBlockTags.SPLITTER_SIDE_OUTPUT);

                if (x != FactoryUtil.MovableResult.FAILURE) {
                    return true;
                }

                x = FactoryUtil.tryInsertingMovable(conveyor, self.getWorld(), conveyorPos, conveyorPos.relative(direction).above(), direction, direction, FactoryBlockTags.SPLITTER_SIDE_OUTPUT);
                if (x != FactoryUtil.MovableResult.FAILURE) {
                    return true;
                }
            }
            return !selfState.getValue(BLOCKING);
        }

        return false;
    }

    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborChanged(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    private void updateEnabled(Level world, BlockPos pos, BlockState state) {
        boolean powered = world.hasNeighborSignal(pos);
        if (powered == state.getValue(ENABLED)) {
            world.setBlock(pos, state.setValue(ENABLED, !powered), 4);
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var below = ctx.getLevel().getBlockState(ctx.getClickedPos().below());

        Direction dir;

        if (below.getBlock() instanceof ConveyorBlock) {
            dir = below.getValue(ConveyorBlock.DIRECTION);
        } else {
            dir = ctx.getHorizontalDirection();
        }

        return waterLog(ctx, this.defaultBlockState().setValue(FACING, dir));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,  BlockHitResult hit) {
        var be = world.getBlockEntity(pos);
        var hand = InteractionHand.MAIN_HAND;
        var stack = player.getItemInHand(hand);
        if ((stack.getItem() instanceof AbstractFilterItem item && item.isFilterSet(stack) || stack.isEmpty()) && be instanceof SplitterBlockEntity splitterBlockEntity) {
            if (!splitterBlockEntity.checkUnlocked(player)) {
                return InteractionResult.FAIL;
            }

            var dir = state.getValue(FACING);
            if (hit.getDirection().getAxis() != Direction.Axis.Y && hit.getDirection().getAxis() != dir.getAxis()) {
                if (hit.getDirection() == dir.getCounterClockWise() && (!splitterBlockEntity.getFilterLeft().isEmpty() || !stack.isEmpty())) {
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, splitterBlockEntity.getFilterLeft());
                        splitterBlockEntity.setFilterLeft(ItemStack.EMPTY);
                    } else {
                        if (!splitterBlockEntity.getFilterLeft().isEmpty()) {
                            player.getInventory().placeItemBackInInventory(splitterBlockEntity.getFilterLeft());
                        }
                        splitterBlockEntity.setFilterLeft(stack.copyWithCount(1));
                        stack.shrink(1);
                        if (player instanceof ServerPlayer serverPlayer) {
                            TriggerCriterion.trigger(serverPlayer, FactoryTriggers.ITEM_FILTER_USE);
                        }
                    }
                    return InteractionResult.SUCCESS_SERVER;
                } else if (!splitterBlockEntity.getFilterRight().isEmpty() || !stack.isEmpty()) {
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, splitterBlockEntity.getFilterRight());
                        splitterBlockEntity.setFilterRight(ItemStack.EMPTY);
                    } else {
                        if (!splitterBlockEntity.getFilterRight().isEmpty()) {
                            player.getInventory().placeItemBackInInventory(splitterBlockEntity.getFilterRight());
                        }
                        splitterBlockEntity.setFilterRight(stack.copyWithCount(1));
                        stack.shrink(1);
                        if (player instanceof ServerPlayer serverPlayer) {
                            TriggerCriterion.trigger(serverPlayer, FactoryTriggers.ITEM_FILTER_USE);
                        }
                    }
                    return InteractionResult.SUCCESS_SERVER;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, FACING);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.SMOOTH_STONE.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SplitterBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING_HORIZONTAL);
    }

    public static final class Model extends BlockModel {
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final FilterIcon leftLockElement = new FilterIcon(this);
        private final FilterIcon rightLockElement = new FilterIcon(this);

        private Model(BlockState state) {
            this.mainElement = ItemDisplayElementUtil.createSolid(state.getBlock().asItem());

            this.updateFacing(state);
            this.addElement(this.mainElement);
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.getValue(FACING).getRotation().mul(Direction.NORTH.getRotation());
            mat.identity();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.scale(2f);
            mat.rotateY(Mth.PI);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(-0.51f, 5.5f / 16f, 0);
            mat.rotateY(Mth.HALF_PI);
            mat.scale(0.3f, 0.3f, 0.005f);
            this.leftLockElement.setTransformation(mat);
            mat.popMatrix();


            mat.pushMatrix();
            mat.translate(0.51f, 5.5f / 16f, 0);
            mat.rotateY(-Mth.HALF_PI);
            mat.scale(0.3f, 0.3f, 0.005f);
            this.rightLockElement.setTransformation(mat);
            mat.popMatrix();

            this.tick();
        }

        public void updateFilters(FilterData filterStackLeft, FilterData filterStackRight) {
            this.leftLockElement.setFilter(filterStackLeft);
            this.rightLockElement.setFilter(filterStackRight);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(this.blockState());
            }
        }
    }
}
