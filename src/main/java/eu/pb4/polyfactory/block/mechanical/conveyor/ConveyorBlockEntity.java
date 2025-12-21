package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.*;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ConveyorBlockEntity extends BlockEntity implements MovingItemContainerHolderProvider, WorldlyContainer, RedirectingMovingItemContainerHolder {
    public double delta;
    @Nullable
    private ConveyorBlock.Model model;
    private MovingItem holdStack = null;

    public ConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CONVEYOR, pos, state);
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        var self = (ConveyorBlockEntity) t;
        var vert = state.getValue(ConveyorBlock.VERTICAL);
        var dir = state.getValue(ConveyorBlock.DIRECTION);
        if (self.model == null) {
            self.model = (ConveyorBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            self.model.updateDelta(-20, self.delta);
            self.model.setContainer(self.holdStack);
        }

        var speed = Math.min(RotationUser.getRotation((ServerLevel) world, pos).speed() * Mth.DEG_TO_RAD * 0.7, 128);

        var itemChanged = self.model.updateSpeed(speed);

        if (self.isContainerEmpty() && speed != 0) {
            if (state.getValue(ConveyorBlock.HAS_OUTPUT_TOP)) {
                var pointer = new WorldPointer(world, pos.above());

                if (pointer.getBlockState().getBlock() instanceof MovingItemProvider provider) {
                    provider.getItemFrom(pointer, dir, Direction.DOWN, pos, self);
                }
            }

            if (self.isContainerEmpty()) {
                var pointer = new WorldPointer(world, pos.relative(dir.getOpposite()));

                if (pointer.getBlockState().getBlock() instanceof MovingItemProvider provider) {
                    provider.getItemFrom(pointer, dir, dir, pos, self);
                }
            }

            if (self.isContainerEmpty()) {
                return;
            }
        }

        if (self.holdStack != null && self.holdStack.get().isEmpty()) {
            self.clearContainer();
            return;
        }

        if (vert != ConveyorBlock.DirectionValue.NONE && !vert.stack) {
            speed = speed / Mth.SQRT_OF_TWO;
        }

        var currentDelta = self.delta;

        if (state.getValue(ConveyorBlock.HAS_OUTPUT_TOP) && !vert.stack) {
            self.delta += speed;
            if (self.tryInserting(world, pos.relative(Direction.UP), Direction.UP, FactoryBlockTags.CONVEYOR_TOP_OUTPUT)) {
                if (self.isContainerEmpty()) {
                    self.setDelta(0);
                }
                return;
            }
            self.delta -= speed;
        }


        if (currentDelta + speed >= 1) {
            boolean block = false;

            self.delta += speed;
            if (!vert.stack) {
                block = self.tryInserting(world, pos.relative(dir), dir, FactoryBlockTags.CONVEYOR_SIDE_OUTPUT);
            }

            if (!block) {
                block = self.tryMovingConveyor(world, pos, state, vert, dir);

                if (!block) {
                    var blockerPos = pos.relative(dir).above();
                    block = world.getBlockState(blockerPos).isFaceSturdy(world, blockerPos, dir.getOpposite());
                }
            }

            if (block) {
                self.delta -= speed;
                if (self.isContainerEmpty()) {
                    self.setDelta(0);
                }
                return;
            }

            var stack = self.getItem(0);
            self.clearContainer();
            var moveVec = Vec3.ZERO.relative(dir, 0.5).add(0, vert.value * 0.6, 0);
            var vec = Vec3.atCenterOf(pos).add(moveVec).add(0, vert == ConveyorBlock.DirectionValue.NONE ? 0.5 : 0, 0);
            moveVec = moveVec.scale(0.5);
            var itemEntity = new ItemEntity(world, vec.x, vec.y, vec.z, stack, moveVec.x, moveVec.y, moveVec.z);
            itemEntity.tickCount = -20;
            world.addFreshEntity(itemEntity);

            self.delta -= speed;

        } else {
            self.setDelta(currentDelta + speed);
        }
    }

    private boolean tryMovingConveyor(Level world, BlockPos pos, BlockState state, ConveyorBlock.DirectionValue vert, Direction dir) {
        BlockPos next;
        if (vert.stack) {
            next = pos.above(vert.value);
            if (vert.value == -1) {
                var maybeNext = next.relative(dir);
                var possibleNext = world.getBlockState(maybeNext);
                if (possibleNext.is(FactoryBlockTags.CONVEYORS)) {
                    next = maybeNext;
                }
            }
        } else if (vert.value == 0 || vert.value == 1) {
            next = pos.relative(dir);
            var possibleNext = world.getBlockState(next);
            if (possibleNext.is(FactoryBlockTags.CONVEYORS)) {
                var maybeNext = next.above();
                var possibleNext2 = world.getBlockState(maybeNext);
                if (possibleNext2.is(FactoryBlockTags.CONVEYORS) && possibleNext2.getValue(ConveyorBlock.VERTICAL).value == 1) {
                    next = maybeNext;
                }
            }
        } else if (vert.value == -1) {
            next = pos.below();
            var possibleNext = world.getBlockState(next);
            if (possibleNext.is(FactoryBlockTags.CONVEYORS)) {
                var maybeNext = next.relative(dir);
                var possibleNext2 = world.getBlockState(maybeNext);
                if (possibleNext2.is(FactoryBlockTags.CONVEYORS)) {
                    next = maybeNext;
                }
            }
        } else {
            return false;
        }

        var nextConveyor = world.getBlockEntity(next) instanceof ConveyorBlockEntity conveyorBlock ? conveyorBlock : null;
        if (nextConveyor == null) {
            return false;
        }

        if (!nextConveyor.isContainerEmpty()) {
            return true;
        }
        var nextState = nextConveyor.getBlockState();

        var nextVert = nextState.getValue(ConveyorBlock.VERTICAL);
        if (nextState.getValue(ConveyorBlock.VERTICAL).stack && !nextState.is(FactoryBlocks.STICKY_CONVEYOR)) {
            return nextVert.value == 1;
        }

        var nextDir = nextState.getValue(ConveyorBlock.DIRECTION);
        if (nextDir == dir) {
            nextConveyor.setDelta(0);
        } else if (nextDir.getCounterClockWise().getAxis() == dir.getAxis()) {
            nextConveyor.setDelta(0.5);
        } else if (nextDir.getOpposite() == dir) {
            nextConveyor.setDelta(0.8);
        }

        /*if (vert.stack && vert.value == -1 && nextState.get(ConveyorBlock.VERTICAL) == ConveyorBlock.DirectionValue.NONE) {
            nextConveyor.setDelta(1);
        }*/

        var x = this.pullAndRemove();
        if (x != null) {
            nextConveyor.pushAndAttach(x);
        }
        return true;
    }

    private boolean tryInserting(Level world, BlockPos pos, Direction dir, TagKey<Block> requiredTag) {
        var x = FactoryUtil.tryInsertingMovable(this, world, this.getBlockPos(), pos, dir, this.getBlockState().getValue(ConveyorBlock.DIRECTION), requiredTag);

        if (this.holdStack == null || this.holdStack.get().isEmpty()) {
            this.clearContainer();
            this.setDelta(0);
        }
        return x != FactoryUtil.MovableResult.FAILURE;
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        if (this.holdStack != null && !this.holdStack.get().isEmpty()) {
            view.store("HeldStack", ItemStack.OPTIONAL_CODEC, this.holdStack.get());
        }
        view.putDouble("Delta", this.delta);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        var stack = view.read("HeldStack", ItemStack.OPTIONAL_CODEC);
        if (stack.isPresent()) {
            if (this.holdStack != null) {
                this.holdStack.set(stack.get());
            } else {
                this.setContainer(new MovingItem(stack.get()));
            }
        }
        this.delta = view.getDoubleOr("Delta", 0);
    }

    public boolean tryAdding(ItemStack stack) {
        if (this.isContainerEmpty()) {
            pushNew(stack);
            this.setDelta(0);
            return true;
        }

        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 16;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    public void setDelta(double delta) {
        if (this.model != null) {
            this.model.updateDelta(this.delta, delta);
        }
        this.delta = delta;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.isContainerEmpty();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        MovingItemContainerHolderProvider.super.setItem(slot, stack);
        this.setDelta(0.5);
    }

    @Override
    public MovingItem getContainer() {
        return this.holdStack;
    }

    @Override
    public double movementDelta() {
        return this.delta;
    }

    @Override
    public void setMovementPosition(double pos) {
        this.setDelta(pos);
    }

    @Override
    public void setContainerLocal(MovingItem container) {
        this.holdStack = container;
        if (this.level != null) {
            this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
            this.setChanged();
        }
    }

    @Override
    public @Nullable MovingItemContainerHolder getRedirect() {
        return this.model;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.holdStack == null || this.holdStack.get().isEmpty() || this.holdStack.get().getCount() + stack.getCount() <= getMaxStackCount(stack);
    }

    @Override
    public int getMaxStackCount(ItemStack stack) {
        return Math.max(stack.getMaxStackSize() / 4, 1);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public MovingItemContainerHolder getContainerHolder(int slot) {
        return this;
    }

    @Override
    public void clearContent() {
        this.clearContainer();
    }
}
