package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.BaseModel;
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
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class MixerBlock extends TallItemMachineBlock {
    public MixerBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.ANVIL.getDefaultState();
    }


    @Override
    protected BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state) {
        return new MixerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.MIXER ? MixerBlockEntity::ticker : null;
    }


    @Override
    protected ElementHolder createModel(ServerWorld serverWorld, BlockPos pos, BlockState initialBlockState) {
        return new Model(serverWorld, initialBlockState);
    }

    @Override
    public boolean pushItemTo(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        if (self.getBlockState().get(INPUT_FACING).getOpposite() != pushDirection || self.getBlockState().get(PART) == Part.TOP) {
            return false;
        }

        var be = (MixerBlockEntity) self.getBlockEntity();

        for (int i = 0; i < MixerBlockEntity.OUTPUT_SLOT; i++) {
            var container = be.getContainerHolder(i);

            if (container.isContainerEmpty()) {
                container.pushAndAttach(conveyor.pullAndRemove());
                return true;
            } else {
                var targetStack = container.getContainer().get();
                var sourceStack = conveyor.getContainer().get();

                if (ItemStack.canCombine(container.getContainer().get(), conveyor.getContainer().get())) {
                    var count = Math.min(targetStack.getCount() + sourceStack.getCount(), container.getMaxStackCount(sourceStack));
                    if (count != targetStack.getCount()) {
                        var dec = count - targetStack.getCount();
                        targetStack.increment(dec);
                        sourceStack.decrement(dec);
                        if (sourceStack.isEmpty()) {
                            conveyor.clearContainer();
                        }
                        return true;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void getItemFrom(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        /*var inputDir = self.getBlockState().get(INPUT_FACING);
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
        }*/
    }

    public final class Model extends BaseModel {
        public static final ItemStack MODEL_PISTON = new ItemStack(Items.CANDLE);

        static {
            MODEL_PISTON.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(MODEL_PISTON.getItem(), FactoryUtil.id("block/mixer_whisk")).value());
        }

        private final Matrix4f mat = new Matrix4f();
        private final ItemDisplayElement whisk;
        private final ItemDisplayElement main;
        private float rotation;
        private boolean active;

        private Model(ServerWorld world, BlockState state) {
            this.main = new LodItemDisplayElement(FactoryItems.PRESS_BLOCK.getDefaultStack());
            this.main.setDisplaySize(1, 1);
            this.main.setModelTransformation(ModelTransformationMode.FIXED);
            this.main.setInvisible(true);

            this.whisk = new LodItemDisplayElement(MODEL_PISTON);
            this.whisk.setDisplaySize(1, 1);
            this.whisk.setModelTransformation(ModelTransformationMode.FIXED);
            this.whisk.setInterpolationDuration(2);
            this.whisk.setInvisible(true);
            this.updateStatePos(state);
            this.updateAnimation();
            this.addElement(this.whisk);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.get(INPUT_FACING);

            this.main.setYaw(direction.asRotation());
            this.whisk.setYaw(direction.asRotation());
        }

        private void updateAnimation() {
            mat.identity();
            mat.translate(0, 0.5f, 0);
            mat.scale(2f);
            this.main.setTransformation(mat);
            mat.rotateY(this.rotation);
            mat.translate(0, this.active ? 0.65f : 0.4f, 0);
            this.whisk.setTransformation(mat);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(BlockBoundAttachment.get(this).getBlockState());
            }
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % 2 == 0) {
                this.updateAnimation();
                if (this.whisk.isDirty()) {
                    this.whisk.startInterpolation();
                }
            }
        }

        public void rotate(float speed) {
            this.rotation += speed * MathHelper.RADIANS_PER_DEGREE;
            if (this.rotation > MathHelper.TAU) {
                this.rotation -= MathHelper.TAU;
            }
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
