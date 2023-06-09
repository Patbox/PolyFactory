package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.FilterItem;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;


public class SplitterBlock extends Block implements PolymerBlock, MovingItemConsumer, BlockEntityProvider, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public static DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = Properties.ENABLED;

    public SplitterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(ENABLED, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public boolean pushItemTo(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        var selfState = self.getBlockState();
        var dir = selfState.get(FACING);
        var item = conveyor.getContainer();

        if (!selfState.get(ENABLED) || item == null || item.get().isEmpty() || conveyor.movementDelta() < 0.49) {
            return false;
        }
        if (self.getBlockEntity() instanceof SplitterBlockEntity blockEntity) {
            Direction direction;
            if (blockEntity.matchesLeft(item.get())) {
                direction = dir.rotateYCounterclockwise();
            } else if (blockEntity.matchesRight(item.get())) {
                direction = dir.rotateYClockwise();
            } else {
                return false;
            }

            var x = FactoryUtil.tryInsertingMovable(conveyor, self.getWorld(),
                    conveyorPos.offset(direction), direction, direction, FactoryBlockTags.CONVEYOR_SIDE_OUTPUT);

            if (x != FactoryUtil.MovableResult.FAILURE) {
                return true;
            }

            x = FactoryUtil.tryInsertingMovable(conveyor, self.getWorld(), conveyorPos.offset(direction).down(), direction, direction, FactoryBlockTags.CONVEYOR_SIDE_OUTPUT);
            if (x != FactoryUtil.MovableResult.FAILURE) {
                return true;
            }

            return true;
        }

        return false;
    }

    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
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

        return this.getDefaultState().with(FACING, dir);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof SplitterBlockEntity be) {
                ItemScatterer.spawn(world, pos.getX()  + 0.5, pos.getY()  + 0.5, pos.getZ() + 0.5, be.getFilterRight());
                ItemScatterer.spawn(world, pos.getX()  + 0.5, pos.getY()  + 0.5, pos.getZ() + 0.5, be.getFilterLeft());
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }


    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var be = world.getBlockEntity(pos);
        var stack = player.getStackInHand(hand);

        if (((stack.isOf(FactoryItems.ITEM_FILTER) && !FilterItem.getStack(stack).isEmpty()) || stack.isEmpty()) && hand == Hand.MAIN_HAND && be instanceof SplitterBlockEntity splitterBlockEntity) {
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
            return ActionResult.SUCCESS;
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
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SplitterBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    public final class Model extends ElementHolder {
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement leftLockElement;
        private final ItemDisplayElement rightLockElement;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.mainElement = new ItemDisplayElement();
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setItem(FactoryItems.SPLITTER_BLOCK.getDefaultStack());
            this.mainElement.setInvisible(true);

            this.leftLockElement = new ItemDisplayElement();
            this.leftLockElement.setDisplaySize(1, 1);
            this.leftLockElement.setModelTransformation(ModelTransformationMode.GUI);
            this.leftLockElement.setInvisible(true);

            this.rightLockElement = new ItemDisplayElement();
            this.rightLockElement.setDisplaySize(1, 1);
            this.rightLockElement.setModelTransformation(ModelTransformationMode.GUI);
            this.rightLockElement.setInvisible(true);


            this.updateFacing(state);
            this.addElement(this.mainElement);
            this.addElement(this.leftLockElement);
            this.addElement(this.rightLockElement);


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

        public void updateFilters(ItemStack filterStackLeft, ItemStack filterStackRight) {
            this.leftLockElement.setItem(filterStackLeft);
            this.rightLockElement.setItem(filterStackRight);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(BlockBoundAttachment.get(this).getBlockState());
            }
        }

        static {
        }
    }
}
