package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.GenericParts;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.Locale;

public class PressBlock extends TallItemMachineBlock {
    public PressBlock(Settings settings) {
        super(settings);
        Model.MODEL_PISTON.isEmpty();
    }

    @Override
    public boolean pushItemTo(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        if (self.getBlockState().get(INPUT_FACING) == pushDirection || self.getBlockState().get(PART) == Part.TOP) {
            return false;
        }

        var be = (PressBlockEntity) self.getBlockEntity();

        if (self.getBlockState().get(INPUT_FACING).getOpposite() != pushDirection) {
            var stack = be.getStack(1);
            if (stack.isEmpty()) {
                be.setStack(1, conveyor.pullAndDestroy().get());
                return true;
            }

            var container = conveyor.getContainer();

            if (ItemStack.canCombine(container.get(), stack)) {
                var i = Math.min(container.get().getCount(), stack.getMaxCount() - stack.getCount());
                stack.increment(i);
                container.get().decrement(i);
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

            if (ItemStack.canCombine(container.getContainer().get(), conveyor.getContainer().get())) {
                var count = Math.min(targetStack.getCount() + sourceStack.getCount(), container.getMaxStackCount(sourceStack));
                if (count != targetStack.getCount()) {
                    var dec = count - targetStack.getCount();
                    targetStack.increment(dec);
                    sourceStack.decrement(dec);
                }

                if (sourceStack.isEmpty()) {
                    conveyor.clearContainer();
                }
            }
        }

        return true;
    }

    @Override
    public void getItemFrom(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        var inputDir = self.getBlockState().get(INPUT_FACING);
        if (!conveyor.isContainerEmpty() || pushDirection == inputDir || inputDir.getOpposite() != relative || self.getBlockState().get(PART) == Part.TOP) {
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
            stack.decrement(amount);
            conveyor.setMovementPosition(pushDirection == inputDir.getOpposite() ? 0 : 0.5);
            conveyor.pushNew(stack.copyWithCount(amount));
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.PRESS ? PressBlockEntity::ticker : null;
    }

    @Override
    protected BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state) {
        return new PressBlockEntity(pos, state);
    }

    @Override
    protected ElementHolder createModel(ServerWorld serverWorld, BlockPos pos, BlockState initialBlockState) {
        return new Model(serverWorld, initialBlockState);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.ANVIL.getDefaultState();
    }

    public final class Model extends BaseModel {
        public static final ItemStack MODEL_PISTON = new ItemStack(Items.CANDLE);

        static {
            MODEL_PISTON.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(MODEL_PISTON.getItem(), FactoryUtil.id("block/press_piston")).value());
        }

        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement piston;
        private final ItemDisplayElement pistonItem;
        private final ItemDisplayElement main;
        private final ItemDisplayElement gearA;
        private final ItemDisplayElement gearB;

        private float value;

        private Model(ServerWorld world, BlockState state) {
            this.main = LodItemDisplayElement.createSimple(FactoryItems.PRESS_BLOCK.getDefaultStack(), 0);
            this.main.setScale(new Vector3f(2));
            this.main.setTranslation(new Vector3f(0, 0.5f, 0));
            this.piston = LodItemDisplayElement.createSimple(MODEL_PISTON, 2, 0.4f, 0.8f);
            this.pistonItem = LodItemDisplayElement.createSimple(ItemStack.EMPTY, 2, 0.4f, 0.8f);
            this.gearA = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR, 4, 0.3f, 0.5f);
            this.gearB = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR, 4, 0.3f, 0.5f);
            this.piston.setViewRange(0.4f);
            this.pistonItem.setViewRange(0.4f);
            this.gearA.setViewRange(0.4f);
            this.gearB.setViewRange(0.4f);

            updateStatePos(state);
            var dir = state.get(INPUT_FACING);
            this.updateAnimation(true, 0, (dir.getDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
            this.addElement(this.piston);
            this.addElement(this.pistonItem);
            this.addElement(this.main);
            this.addElement(this.gearA);
            this.addElement(this.gearB);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.get(INPUT_FACING);

            this.main.setYaw(direction.asRotation());
            this.pistonItem.setYaw(direction.asRotation());
            this.piston.setYaw(direction.asRotation());
            this.gearA.setYaw(direction.asRotation());
            this.gearB.setYaw(direction.asRotation());
        }

        private void updateAnimation(boolean b, float rotation, boolean negative) {
            mat.identity();
            mat.translate(0, 0.5f, 0);
            if (b) {
                mat.pushMatrix();
                mat.rotateY(negative ? MathHelper.HALF_PI : -MathHelper.HALF_PI);
                mat.translate(0, 0.5f, 0.40f);
                mat.rotateZ(rotation);
                this.gearA.setTransformation(mat);
                mat.translate(0, 0, -0.80f);
                this.gearB.setTransformation(mat);
                mat.popMatrix();
            }
            mat.scale(2f);

            mat.translate(0, 0.5f - this.value * 0.3f, 0);
            this.piston.setTransformation(mat);
            mat.translate(0,  -0.25f, 0);
            mat.scale(0.2f);
            mat.rotateX(MathHelper.HALF_PI);
            this.pistonItem.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getTick();

            if (tick % 2 == 0) {
                var b = tick % 4 == 0;

                var dir = BlockBoundAttachment.get(this).getBlockState().get(INPUT_FACING);
                this.updateAnimation(b,
                        b ? RotationUser.getRotation(this.getAttachment().getWorld(), BlockBoundAttachment.get(this).getBlockPos().up()).rotation() : 0,
                        (dir.getDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));

                if (this.piston.isDirty()) {
                    this.piston.startInterpolation();
                    this.pistonItem.startInterpolation();
                }

                if (this.gearA.isDirty()) {
                    this.gearA.startInterpolation();
                    this.gearB.startInterpolation();
                }
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(BlockBoundAttachment.get(this).getBlockState());
            }
        }

        public void updatePiston(double i) {
            if (i < 0) {
                this.value = (float) Math.min(-i * 5f, 1);
            } else {
                this.value = (float) Math.min(i * 1.3, 1);
            }
        }

        public void setItem(ItemStack stack) {
            this.pistonItem.setItem(stack);
        }
    }
}
