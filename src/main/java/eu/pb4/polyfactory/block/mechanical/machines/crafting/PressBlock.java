package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.GenericParts;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class PressBlock extends TallItemMachineBlock {
    public PressBlock(Properties settings) {
        super(settings);
        Model.MODEL_PISTON.isEmpty();
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        if (self.getBlockState().getValue(INPUT_FACING) == pushDirection || self.getBlockState().getValue(PART) == Part.TOP) {
            return false;
        }

        var be = (PressBlockEntity) self.getBlockEntity();

        if (self.getBlockState().getValue(INPUT_FACING).getOpposite() != pushDirection) {
            var stack = be.getItem(1);
            if (stack.isEmpty()) {
                be.setItem(1, conveyor.pullAndDestroy().get());
                return true;
            }

            var container = conveyor.getContainer();

            if (ItemStack.isSameItemSameComponents(container.get(), stack)) {
                var i = Math.min(container.get().getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(i);
                container.get().shrink(i);
            }

            if (container.get().isEmpty()) {
                conveyor.clearContainer();
            }

            return true;
        }

        var container = be.getContainerHolder(0);

        if (container.isContainerEmpty()) {
            container.pushAndAttach(conveyor.pullAndRemove());
        } else {
            var targetStack = container.getContainer().get();
            var sourceStack = conveyor.getContainer().get();

            if (ItemStack.isSameItemSameComponents(container.getContainer().get(), conveyor.getContainer().get())) {
                var count = Math.min(targetStack.getCount() + sourceStack.getCount(), container.getMaxStackCount(sourceStack));
                if (count != targetStack.getCount()) {
                    var dec = count - targetStack.getCount();
                    targetStack.grow(dec);
                    sourceStack.shrink(dec);
                }

                if (sourceStack.isEmpty()) {
                    conveyor.clearContainer();
                }
            }
        }

        return true;
    }

    @Override
    public void getItemFrom(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        var inputDir = self.getBlockState().getValue(INPUT_FACING);
        if (!conveyor.isContainerEmpty() || pushDirection == inputDir || inputDir.getOpposite() != relative || self.getBlockState().getValue(PART) == Part.TOP) {
            return;
        }

        var be = (PressBlockEntity) self.getBlockEntity();

        var out = be.getContainerHolder(2);

        if (out.isContainerEmpty()) {
            return;
        }
        var stack = out.getContainer().get();

        var amount = Math.min(stack.getCount(), out.getMaxStackCount(stack));

        if (stack.getCount() == amount) {
            conveyor.pushAndAttach(out.pullAndRemove());
        } else {
            stack.shrink(amount);
            conveyor.setMovementPosition(pushDirection == inputDir.getOpposite() ? 0 : 0.5);
            conveyor.pushNew(stack.copyWithCount(amount));
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.PRESS ? PressBlockEntity::ticker : null;
    }

    @Override
    protected BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state) {
        return new PressBlockEntity(pos, state);
    }

    @Override
    protected ElementHolder createModel(ServerLevel serverWorld, BlockPos pos, BlockState initialBlockState) {
        return new Model(serverWorld, initialBlockState);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.ANVIL.defaultBlockState();
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack MODEL_PISTON =  ItemDisplayElementUtil.getModel(FactoryUtil.id("block/press_piston"));


        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement piston;
        private final ItemDisplayElement pistonItem;
        private final ItemDisplayElement main;
        private final ItemDisplayElement gearA;
        private final ItemDisplayElement gearB;

        private float value;

        private Model(ServerLevel world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(FactoryItems.PRESS);
            this.main.setScale(new Vector3f(2));
            this.main.setTranslation(new Vector3f(0, 0.5f, 0));
            this.piston = LodItemDisplayElement.createSimple(MODEL_PISTON, 2, 0.4f, 0.8f);
            this.pistonItem = LodItemDisplayElement.createSimple(ItemStack.EMPTY, 2, 0.4f, 0.8f);
            this.gearA = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR, this.getUpdateRate(), 0.3f, 0.5f);
            this.gearB = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR, this.getUpdateRate(), 0.3f, 0.5f);
            this.piston.setViewRange(0.4f);
            this.pistonItem.setViewRange(0.4f);
            this.gearA.setViewRange(0.4f);
            this.gearB.setViewRange(0.4f);

            updateStatePos(state);
            var dir = state.getValue(INPUT_FACING);
            this.updateAnimation(true, true, 0, (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
            this.addElement(this.piston);
            this.addElement(this.pistonItem);
            this.addElement(this.main);
            this.addElement(this.gearA);
            this.addElement(this.gearB);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(INPUT_FACING);

            this.main.setYaw(direction.toYRot());
            this.pistonItem.setYaw(direction.toYRot());
            this.piston.setYaw(direction.toYRot());
            this.gearA.setYaw(direction.toYRot());
            this.gearB.setYaw(direction.toYRot());
        }

        private void updateAnimation(boolean b, boolean c, float rotation, boolean negative) {
            mat.identity();
            mat.translate(0, 0.5f, 0);
            if (b) {
                mat.pushMatrix();
                mat.rotateY(negative ? Mth.HALF_PI : -Mth.HALF_PI);
                mat.translate(0, 0.5f, 0.40f);
                mat.rotateZ(rotation);
                this.gearA.setTransformation(mat);
                mat.translate(0, 0, -0.80f);
                this.gearB.setTransformation(mat);
                mat.popMatrix();
            }

            if (c) {
                mat.scale(2f);

                mat.translate(0, 0.5f - this.value * 0.3f, 0);
                this.piston.setTransformation(mat);
                mat.translate(0, -0.25f, 0);
                mat.scale(0.2f);
                mat.rotateX(Mth.HALF_PI);
                this.pistonItem.setTransformation(mat);
            }
        }

        @Override
        protected void onTick() {
            var tick = this.getTick();
            var b = tick % this.getUpdateRate() == 0;
            var c = tick % 2 == 0;
            var dir = this.blockState().getValue(INPUT_FACING);


            this.updateAnimation(b, c,
                    b ? RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos().above()).rotation() : 0,
                    (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));

            if (c) {
                this.piston.startInterpolationIfDirty();
                this.pistonItem.startInterpolationIfDirty();
            }

            if (b) {
                this.gearA.startInterpolationIfDirty();
                this.gearB.startInterpolationIfDirty();
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        public void updatePiston(double i) {
            if (i < 0) {
                this.value = (float) Math.min(-i * 1.7f, 1);
            } else {
                this.value = (float) Math.min(i * 2.2, 1);
            }
        }

        public void setItem(ItemStack stack) {
            this.pistonItem.setItem(stack);
        }
    }
}
