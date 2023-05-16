package eu.pb4.polyfactory.block.mechanical.conveyor;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.mechanical.RotationalSource;
import eu.pb4.polyfactory.util.CachedBlockPointer;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.MovingItemContainer;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ConveyorBlockEntity extends BlockEntity implements SingleStackInventory, SidedInventory, MovingItemContainer.AwareRedirecting {
    public double delta;
    @Nullable
    private ConveyorBlock.Model model;
    private MovingItemContainer holdStack = null;

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

        var speed = RotationalSource.getNetworkSpeed((ServerWorld) world, pos);

        var itemChanged = self.model.updateSpeed(speed);

        if (self.isContainerEmpty() && speed != 0) {
            if (state.get(ConveyorBlock.HAS_OUTPUT_TOP)) {
                var pointer = new CachedBlockPointer(world, pos.up());

                if (pointer.getBlockState().getBlock() instanceof MovingItemProvider provider) {
                    provider.getItemFrom(pointer, dir, Direction.DOWN, pos, self);
                }
            }

            if (self.isContainerEmpty()) {
                if (itemChanged) {
                    self.model.tick();
                }
                return;
            }
        }

        if (self.holdStack != null && self.holdStack.get().isEmpty()) {
            self.clearContainer();
            self.model.tick();
            return;
        }

        if (speed == 0) {
            self.model.tick();
            return;
        }

        if (vert != ConveyorBlock.DirectionValue.NONE && !vert.stack) {
            speed = speed / MathHelper.SQUARE_ROOT_OF_TWO;
        }

        var currentDelta = self.delta;

        if (state.get(ConveyorBlock.HAS_OUTPUT_TOP)) {
            if (self.tryInserting(world, pos.offset(Direction.UP), Direction.UP, FactoryBlockTags.CONVEYOR_TOP_OUTPUT)) {
                if (self.isContainerEmpty()) {
                    self.setDelta(0);
                }
                self.model.tick();
                return;
            }
        }


        if (currentDelta + speed >= 1) {
            boolean block;

            if (vert.stack) {
                block = self.tryInserting(world, pos.up(vert.value), dir, null);
            } else {
                if (vert.value == -1) {
                    block = self.tryInserting(world, pos.offset(dir).down(), dir, FactoryBlockTags.CONVEYOR_SIDE_OUTPUT);
                } else {
                    block = self.tryInserting(world, pos.offset(dir).up(), dir, FactoryBlockTags.CONVEYOR_SIDE_OUTPUT) || self.tryInserting(world, pos.offset(dir), dir, FactoryBlockTags.CONVEYOR_SIDE_OUTPUT);
                }
            }


            if (block) {
                if (self.isContainerEmpty()) {
                    self.setDelta(0);
                }
                self.model.tick();
                return;
            }

            var stack = self.getStack();
            self.clearContainer();
            var moveVec = Vec3d.ZERO.offset(dir, 0.5).add(0, vert.value * 0.6, 0);
            var vec = Vec3d.ofCenter(pos).add(moveVec).add(0, vert == ConveyorBlock.DirectionValue.NONE ? 0.5 : 0, 0);
            moveVec = moveVec.multiply(0.5);
            var itemEntity = new ItemEntity(world, vec.x, vec.y, vec.z, stack, moveVec.x, moveVec.y, moveVec.z);
            world.spawnEntity(itemEntity);

            self.model.tick();
        } else {
            self.setDelta(currentDelta + speed);
            self.model.tick();
        }
    }

    private boolean tryInserting(World world, BlockPos pos, Direction dir, TagKey<Block> requiredTag) {
        var x = FactoryUtil.tryInsertingMovable(this, world, pos, dir, this.getCachedState().get(ConveyorBlock.DIRECTION), requiredTag);

        if (x == FactoryUtil.MovableResult.SUCCESS_REGULAR) {
            this.setStack(ItemStack.EMPTY);
            this.setDelta(0);
            return true;
        }
        return x != FactoryUtil.MovableResult.FAILURE;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        if (this.holdStack != null) {
            nbt.put("HeldStack", this.holdStack.get().writeNbt(new NbtCompound()));
        }
        nbt.putDouble("Delta", this.delta);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        var stack = ItemStack.fromNbt(nbt.getCompound("HeldStack"));
        if (!stack.isEmpty()) {
            if (this.holdStack != null) {
                this.holdStack.set(stack);
            } else {
                this.setContainer(new MovingItemContainer(stack));
            }
        }
        this.delta = nbt.getDouble("Delta");
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

    public void setDelta(double delta) {
        if (this.model != null) {
            this.model.updateDelta(this.delta, delta);
        }
        this.delta = delta;
    }

    @Override
    public ItemStack getStack() {
        return this.holdStack != null ? this.holdStack.get() : ItemStack.EMPTY;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.setContainer(new MovingItemContainer(stack));
        this.setDelta(0.5);
        this.markDirty();
        assert this.world != null;
        this.world.updateComparators(this.pos, this.getCachedState().getBlock());
    }

    @Override
    public MovingItemContainer getContainer() {
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
    public void setContainerLocal(MovingItemContainer container) {
        this.holdStack = container;
    }

    @Override
    public @Nullable MovingItemContainer.Aware getRedirect() {
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
}
