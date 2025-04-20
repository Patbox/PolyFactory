package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.*;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ConveyorBlockEntity extends BlockEntity implements InventoryContainerHolderProvider, SidedInventory, RedirectingContainerHolder {
    public double delta;
    @Nullable
    private ConveyorBlock.Model model;
    private MovingItem holdStack = null;

    public ConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CONVEYOR, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        var self = (ConveyorBlockEntity) t;
        var vert = state.get(ConveyorBlock.VERTICAL);
        var dir = state.get(ConveyorBlock.DIRECTION);
        if (self.model == null) {
            self.model = (ConveyorBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            self.model.updateDelta(-20, self.delta);
            self.model.setContainer(self.holdStack);
        }

        var speed = Math.min(RotationUser.getRotation((ServerWorld) world, pos).speed() * MathHelper.RADIANS_PER_DEGREE * 0.7, 128);

        var itemChanged = self.model.updateSpeed(speed);

        if (self.isContainerEmpty() && speed != 0) {
            if (state.get(ConveyorBlock.HAS_OUTPUT_TOP)) {
                var pointer = new WorldPointer(world, pos.up());

                if (pointer.getBlockState().getBlock() instanceof MovingItemProvider provider) {
                    provider.getItemFrom(pointer, dir, Direction.DOWN, pos, self);
                }
            }

            if (self.isContainerEmpty()) {
                var pointer = new WorldPointer(world, pos.offset(dir.getOpposite()));

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
            speed = speed / MathHelper.SQUARE_ROOT_OF_TWO;
        }

        var currentDelta = self.delta;

        if (state.get(ConveyorBlock.HAS_OUTPUT_TOP) && !vert.stack) {
            self.delta += speed;
            if (self.tryInserting(world, pos.offset(Direction.UP), Direction.UP, FactoryBlockTags.CONVEYOR_TOP_OUTPUT)) {
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
                block = self.tryInserting(world, pos.offset(dir), dir, FactoryBlockTags.CONVEYOR_SIDE_OUTPUT);
            }

            if (!block) {
                block = self.tryMovingConveyor(world, pos, state, vert, dir);

                if (!block) {
                    var blockerPos = pos.offset(dir).up();
                    block = world.getBlockState(blockerPos).isSideSolidFullSquare(world, blockerPos, dir.getOpposite());
                }
            }

            if (block) {
                self.delta -= speed;
                if (self.isContainerEmpty()) {
                    self.setDelta(0);
                }
                return;
            }

            var stack = self.getStack(0);
            self.clearContainer();
            var moveVec = Vec3d.ZERO.offset(dir, 0.5).add(0, vert.value * 0.6, 0);
            var vec = Vec3d.ofCenter(pos).add(moveVec).add(0, vert == ConveyorBlock.DirectionValue.NONE ? 0.5 : 0, 0);
            moveVec = moveVec.multiply(0.5);
            var itemEntity = new ItemEntity(world, vec.x, vec.y, vec.z, stack, moveVec.x, moveVec.y, moveVec.z);
            itemEntity.age = -20;
            world.spawnEntity(itemEntity);

            self.delta -= speed;

        } else {
            self.setDelta(currentDelta + speed);
        }
    }

    private boolean tryMovingConveyor(World world, BlockPos pos, BlockState state, ConveyorBlock.DirectionValue vert, Direction dir) {
        BlockPos next;
        if (vert.stack) {
            next = pos.up(vert.value);
            if (vert.value == -1) {
                var maybeNext = next.offset(dir);
                var possibleNext = world.getBlockState(maybeNext);
                if (possibleNext.isIn(FactoryBlockTags.CONVEYORS)) {
                    next = maybeNext;
                }
            }
        } else if (vert.value == 0 || vert.value == 1) {
            next = pos.offset(dir);
            var possibleNext = world.getBlockState(next);
            if (possibleNext.isIn(FactoryBlockTags.CONVEYORS)) {
                var maybeNext = next.up();
                var possibleNext2 = world.getBlockState(maybeNext);
                if (possibleNext2.isIn(FactoryBlockTags.CONVEYORS) && possibleNext2.get(ConveyorBlock.VERTICAL).value == 1) {
                    next = maybeNext;
                }
            }
        } else if (vert.value == -1) {
            next = pos.down();
            var possibleNext = world.getBlockState(next);
            if (possibleNext.isIn(FactoryBlockTags.CONVEYORS)) {
                var maybeNext = next.offset(dir);
                var possibleNext2 = world.getBlockState(maybeNext);
                if (possibleNext2.isIn(FactoryBlockTags.CONVEYORS)) {
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
        var nextState = nextConveyor.getCachedState();

        var nextVert = nextState.get(ConveyorBlock.VERTICAL);
        if (nextState.get(ConveyorBlock.VERTICAL).stack && !nextState.isOf(FactoryBlocks.STICKY_CONVEYOR)) {
            return nextVert.value == 1;
        }

        var nextDir = nextState.get(ConveyorBlock.DIRECTION);
        if (nextDir == dir) {
            nextConveyor.setDelta(0);
        } else if (nextDir.rotateYCounterclockwise().getAxis() == dir.getAxis()) {
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

    private boolean tryInserting(World world, BlockPos pos, Direction dir, TagKey<Block> requiredTag) {
        var x = FactoryUtil.tryInsertingMovable(this, world, this.getPos(), pos, dir, this.getCachedState().get(ConveyorBlock.DIRECTION), requiredTag);

        if (this.holdStack == null || this.holdStack.get().isEmpty()) {
            this.clearContainer();
            this.setDelta(0);
        }
        return x != FactoryUtil.MovableResult.FAILURE;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (this.holdStack != null && !this.holdStack.get().isEmpty()) {
            nbt.put("HeldStack", this.holdStack.get().toNbt(lookup));
        }
        nbt.putDouble("Delta", this.delta);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        var stack = FactoryUtil.fromNbtStack(lookup, nbt.getCompoundOrEmpty("HeldStack"));
        if (!stack.isEmpty()) {
            if (this.holdStack != null) {
                this.holdStack.set(stack);
            } else {
                this.setContainer(new MovingItem(stack));
            }
        }
        this.delta = nbt.getDouble("Delta", 0);
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
    public int getMaxCountPerStack() {
        return 16;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    public void setDelta(double delta) {
        if (this.model != null) {
            this.model.updateDelta(this.delta, delta);
        }
        this.delta = delta;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.isContainerEmpty();
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        InventoryContainerHolderProvider.super.setStack(slot, stack);
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
        if (this.world != null) {
            this.world.updateComparators(this.pos, this.getCachedState().getBlock());
            this.markDirty();
        }
    }

    @Override
    public @Nullable ContainerHolder getRedirect() {
        return this.model;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[] { 0 };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.holdStack == null || this.holdStack.get().isEmpty() || this.holdStack.get().getCount() + stack.getCount() <= getMaxStackCount(stack);
    }

    @Override
    public int getMaxStackCount(ItemStack stack) {
        return Math.max(stack.getMaxCount() / 4, 1);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public ContainerHolder getContainerHolder(int slot) {
        return this;
    }

    @Override
    public void clear() {
        this.clearContainer();
    }
}
