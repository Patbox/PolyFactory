package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.models.fluid.TopFluidViewModel;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import static eu.pb4.polyfactory.ModInit.id;

public class MDrainBlock extends TallItemMachineBlock implements PipeConnectable {
    public MDrainBlock(Properties settings) {
        super(settings);
        Model.AXLE_MODEL.isEmpty();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.ANVIL.defaultBlockState();
    }


    @Override
    protected BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state) {
        return new MDrainBlockEntity(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof MDrainBlockEntity be) {
            return be.getComparatorOutput(state, world, pos, direction);
        }
        return 0;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.MECHANICAL_DRAIN ? MDrainBlockEntity::ticker : null;
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

        var be = (MDrainBlockEntity) self.getBlockEntity();

        for (int i = 0; i < MDrainBlockEntity.OUTPUT_FIRST; i++) {
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
        var inputDir = self.getBlockState().getValue(INPUT_FACING);
        if (!conveyor.isContainerEmpty() || pushDirection == inputDir || inputDir.getOpposite() != relative || self.getBlockState().getValue(PART) == Part.TOP) {
            return;
        }

        var be = (MDrainBlockEntity) self.getBlockEntity();

        var out = be.getContainerHolder(MDrainBlockEntity.OUTPUT_FIRST);

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

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(PART) == Part.MAIN;
    }


    public static final class Model extends RotationAwareModel {
        private static final ItemStack BASE_MODEL = ItemDisplayElementUtil.getSolidModel(id("block/mechanical_drain"));
        private static final ItemStack AXLE_MODEL = ItemDisplayElementUtil.getSolidModel(id("block/mechanical_drain_axle"));
        private final ItemDisplayElement catalyst;
        private final ItemDisplayElement main;
        private final ItemDisplayElement axle;
        private final TopFluidViewModel fluid;
        private float rotation;
        private boolean active;

        private Model(BlockState state) {
            this.fluid = new TopFluidViewModel(this, -7f / 16f + 0.1f, 11f / 16f, 0.5f);
            this.main = ItemDisplayElementUtil.createSimple(BASE_MODEL);
            this.main.setScale(new Vector3f(2));
            this.main.setTranslation(new Vector3f(0, 0.5f, 0));
            this.catalyst = ItemDisplayElementUtil.createSimple();
            this.catalyst.setPitch(-90);
            this.catalyst.setTranslation(new Vector3f(0, 0, 4.5f / 16f));
            this.catalyst.setScale(new Vector3f(12 / 16f));
            this.catalyst.setViewRange(0.4f);

            this.axle = LodItemDisplayElement.createSimple(AXLE_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.axle.setOffset(new Vec3(0, 1, 0));
            this.axle.setViewRange(0.6f);
            this.updateStatePos(state);
            var dir = state.getValue(INPUT_FACING);
            this.updateAnimation(0, dir.getClockWise().getAxis());
            this.addElement(this.main);
            this.addElement(this.axle);
            this.addElement(this.catalyst);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(INPUT_FACING);

            this.main.setYaw(direction.toYRot());
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
            }

            mat.rotateY(rotation);
            mat.scale(2);

            this.axle.setTransformation(mat);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();
            if (tick % this.getUpdateRate() == 0) {
                var dir = this.blockState().getValue(INPUT_FACING);
                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos().above()).rotation(), dir.getClockWise().getAxis());
                this.axle.startInterpolationIfDirty();
            }
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

        public void setCatalyst(ItemStack catalyst) {
            this.catalyst.setItem(catalyst);
        }
    }
}
