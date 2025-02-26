package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.AbstractFilterItem;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.models.FilterIcon;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
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
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;


public class SplitterBlock extends Block implements FactoryBlock, MovingItemConsumer, WrenchableBlock, BlockEntityProvider, BarrierBasedWaterloggable {
    public static final BooleanProperty ENABLED = Properties.ENABLED;
    public static final BooleanProperty DISTRIBUTE = BooleanProperty.of("distribute");
    public static final BooleanProperty BLOCKING = BooleanProperty.of("blocking");
    public static EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public SplitterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(ENABLED, true).with(DISTRIBUTE, true).with(BLOCKING, true));
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED, DISTRIBUTE, BLOCKING);
        builder.add(WATERLOGGED);

    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        var selfState = self.getBlockState();
        var dir = selfState.get(FACING);
        var item = conveyor.getContainer();

        if (!selfState.get(ENABLED) || item == null || item.get().isEmpty() || conveyor.movementDelta() < 0.49) {
            return false;
        }
        if (self.getBlockEntity() instanceof SplitterBlockEntity blockEntity) {
            List<Direction> dirs;

            var stack = item.get();

            if (blockEntity.filtersEmpty()) {
                dirs = List.of(dir.rotateYCounterclockwise(), dir, dir.rotateYClockwise());
            } else if (blockEntity.matchesSides(stack)) {
                dirs = List.of(dir.rotateYCounterclockwise(), dir.rotateYClockwise());
            } else if (blockEntity.matchesLeft(item.get())) {
                dirs = List.of(dir.rotateYCounterclockwise());
            } else if (blockEntity.matchesRight(item.get())) {
                dirs = List.of(dir.rotateYClockwise());
            } else if (blockEntity.isLeftFilterEmpty()) {
                dirs = List.of(dir.rotateYCounterclockwise(), dir);
            } else if (blockEntity.isRightFilterEmpty()) {
                dirs = List.of(dir.rotateYClockwise(), dir);
            } else {
                return false;
            }

            if (selfState.get(DISTRIBUTE) && dirs.size() > 1) {
                dirs = List.of(dirs.get(blockEntity.pos(dirs.size())));
            }

            for (var direction : dirs) {
                var x = FactoryUtil.tryInsertingMovable(conveyor, self.getWorld(), conveyorPos, conveyorPos.offset(direction), direction, direction, FactoryBlockTags.SPLITTER_SIDE_OUTPUT);

                if (x != FactoryUtil.MovableResult.FAILURE) {
                    return true;
                }

                x = FactoryUtil.tryInsertingMovable(conveyor, self.getWorld(), conveyorPos, conveyorPos.offset(direction).up(), direction, direction, FactoryBlockTags.SPLITTER_SIDE_OUTPUT);
                if (x != FactoryUtil.MovableResult.FAILURE) {
                    return true;
                }
            }
            return !selfState.get(BLOCKING);
        }

        return false;
    }

    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    private void updateEnabled(World world, BlockPos pos, BlockState state) {
        boolean powered = world.isReceivingRedstonePower(pos);
        if (powered == state.get(ENABLED)) {
            world.setBlockState(pos, state.with(ENABLED, !powered), 4);
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var below = ctx.getWorld().getBlockState(ctx.getBlockPos().down());

        Direction dir;

        if (below.getBlock() instanceof ConveyorBlock) {
            dir = below.get(ConveyorBlock.DIRECTION);
        } else {
            dir = ctx.getHorizontalPlayerFacing();
        }

        return waterLog(ctx, this.getDefaultState().with(FACING, dir));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof SplitterBlockEntity be) {
                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, be.getFilterRight());
                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, be.getFilterLeft());
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }


    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,  BlockHitResult hit) {
        var be = world.getBlockEntity(pos);
        var hand = Hand.MAIN_HAND;
        var stack = player.getStackInHand(hand);
        if ((stack.getItem() instanceof AbstractFilterItem item && item.isFilterSet(stack) || stack.isEmpty()) && be instanceof SplitterBlockEntity splitterBlockEntity) {
            var dir = state.get(FACING);
            if (hit.getSide().getAxis() != Direction.Axis.Y && hit.getSide().getAxis() != dir.getAxis()) {
                if (hit.getSide() == dir.rotateYCounterclockwise()) {
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, splitterBlockEntity.getFilterLeft());
                        splitterBlockEntity.setFilterLeft(ItemStack.EMPTY);
                    } else {
                        if (!splitterBlockEntity.getFilterLeft().isEmpty()) {
                            player.getInventory().offerOrDrop(splitterBlockEntity.getFilterLeft());
                        }
                        splitterBlockEntity.setFilterLeft(stack.copyWithCount(1));
                        stack.decrement(1);
                    }
                } else {
                    if (stack.isEmpty()) {
                        player.setStackInHand(hand, splitterBlockEntity.getFilterRight());
                        splitterBlockEntity.setFilterRight(ItemStack.EMPTY);
                    } else {
                        if (!splitterBlockEntity.getFilterRight().isEmpty()) {
                            player.getInventory().offerOrDrop(splitterBlockEntity.getFilterRight());
                        }
                        splitterBlockEntity.setFilterRight(stack.copyWithCount(1));
                        stack.decrement(1);
                    }
                }
            }
            return ActionResult.SUCCESS_SERVER;
        }

        return ActionResult.FAIL;
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
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.SMOOTH_STONE.getDefaultState();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SplitterBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(WrenchAction.FACING_HORIZONTAL);
    }

    public static final class Model extends BlockModel {
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final FilterIcon leftLockElement = new FilterIcon(this);
        private final FilterIcon rightLockElement = new FilterIcon(this);

        private Model(BlockState state) {
            this.mainElement = new ItemDisplayElement();
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setItem(FactoryItems.SPLITTER.getDefaultStack());
            this.mainElement.setInvisible(true);
            this.mainElement.setViewRange(0.8f);

            this.updateFacing(state);
            this.addElement(this.mainElement);
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.get(FACING).getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion());
            mat.identity();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.scale(2f);
            mat.rotateY(MathHelper.PI);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(-0.51f, 0.4f, 0);
            mat.rotateY(MathHelper.HALF_PI);
            mat.scale(0.3f, 0.3f, 0.02f);
            this.leftLockElement.setTransformation(mat);
            mat.popMatrix();


            mat.pushMatrix();
            mat.translate(0.51f, 0.4f, 0);
            mat.rotateY(-MathHelper.HALF_PI);
            mat.scale(0.3f, 0.3f, 0.02f);
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
