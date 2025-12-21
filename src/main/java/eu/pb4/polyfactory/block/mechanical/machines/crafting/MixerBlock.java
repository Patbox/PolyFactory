package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.GenericParts;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.models.fluid.TopFluidViewModel;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class MixerBlock extends TallItemMachineBlock implements PipeConnectable {
    public MixerBlock(Properties settings) {
        super(settings);
        Model.MODEL_PISTON.isEmpty();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.ANVIL.defaultBlockState();
    }


    @Override
    protected BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state) {
        return new MixerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.MIXER ? MixerBlockEntity::ticker : null;
    }


    @Override
    protected ElementHolder createModel(ServerLevel serverWorld, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        if (self.getBlockState().getValue(INPUT_FACING).getOpposite() != pushDirection || self.getBlockState().getValue(PART) == Part.TOP) {
            return false;
        }

        var be = (MixerBlockEntity) self.getBlockEntity();

        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var container = be.getContainerHolder(i);

            if (container.isContainerEmpty()) {
                container.pushAndAttach(conveyor.pullAndRemove());
                return true;
            } else {
                var targetStack = container.getContainer().get();
                var sourceStack = conveyor.getContainer().get();

                if (ItemStack.isSameItemSameComponents(container.getContainer().get(), conveyor.getContainer().get())) {
                    var count = Math.min(targetStack.getCount() + sourceStack.getCount(), container.getMaxStackCount(sourceStack));
                    if (count != targetStack.getCount()) {
                        var dec = count - targetStack.getCount();
                        targetStack.grow(dec);
                        sourceStack.shrink(dec);
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
    public void getItemFrom(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {

    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(PART) == Part.MAIN;
    }


    public static final class Model extends RotationAwareModel {
        public static final ItemStack MODEL_PISTON = ItemDisplayElementUtil.getModel(FactoryUtil.id("block/mixer_whisk"));

        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement whisk;
        private final ItemDisplayElement main;
        private final TopFluidViewModel fluid;
        private final ItemDisplayElement gearA;
        private final ItemDisplayElement gearB;
        private float rotation;
        private boolean active;

        private Model(BlockState state) {
            this.fluid = new TopFluidViewModel(this, -4f / 16f, 10f / 16f, 0.5f);
            this.main = ItemDisplayElementUtil.createSimple(FactoryItems.MIXER);
            this.main.setScale(new Vector3f(2));
            this.main.setTranslation(new Vector3f(0, 0.5f, 0));
            this.whisk = LodItemDisplayElement.createSimple(MODEL_PISTON, 2, 0.4f, 0.8f);
            this.gearA = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR, this.getUpdateRate(), 0.3f, 0.5f);
            this.gearB = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR, this.getUpdateRate(), 0.3f, 0.5f);

            this.whisk.setViewRange(0.4f);
            this.gearA.setViewRange(0.4f);
            this.gearB.setViewRange(0.4f);

            this.updateStatePos(state);
            var dir = state.getValue(INPUT_FACING);
            this.updateAnimation(true,  true, 0, (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
            this.addElement(this.whisk);
            this.addElement(this.main);
            this.addElement(this.gearA);
            this.addElement(this.gearB);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(INPUT_FACING);

            this.main.setYaw(direction.toYRot());
            this.whisk.setYaw(direction.toYRot());
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
                mat.rotateY(this.rotation);
                mat.translate(0, this.active ? -0.1f : 0.2f, 0);
                this.whisk.setTransformation(mat);
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        @Override
        protected void onTick() {
            var b = this.getTick() % this.getUpdateRate() == 0;

            var c = this.getTick() % 2 == 0;

            var dir = this.blockState().getValue(INPUT_FACING);
            this.updateAnimation(b, c,
                    b ? RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos().above()).rotation() : 0,
                    (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
            this.whisk.startInterpolationIfDirty();


            this.gearA.startInterpolationIfDirty();
            this.gearB.startInterpolationIfDirty();
        }

        public void setFluid(@Nullable FluidInstance<?> type, float position) {
            this.fluid.setFluid(type, position);
        }

        public void rotate(float speed) {
            this.rotation += speed * Mth.DEG_TO_RAD * 2;
            if (this.rotation > Mth.TWO_PI) {
                this.rotation -= Mth.TWO_PI;
            }
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
