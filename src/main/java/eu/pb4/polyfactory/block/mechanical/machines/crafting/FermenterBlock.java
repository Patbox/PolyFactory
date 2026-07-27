package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.models.GenericParts;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemProvider;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
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
import org.joml.Vector3f;

public class FermenterBlock extends TallItemMachineBlock implements PipeConnectable, MovingItemConsumer, MovingItemProvider {
    public FermenterBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.SMOOTH_STONE.defaultBlockState();
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        if (self.getBlockState().getValue(INPUT_FACING) == pushDirection || self.getBlockState().getValue(PART) == Part.TOP || conveyor.isContainerEmpty()) {
            return false;
        }

        var be = (FermenterBlockEntity) self.getBlockEntity();

        var stack = conveyor.getContainer().get();

        if (FactoryUtil.insertBetween(be, 0, FermenterBlockEntity.OUTPUT_FIRST, stack) == -1) {
            return false;
        }

        if (stack.isEmpty()) {
            conveyor.clearContainer();
        }

        return true;
    }

    @Override
    public void getItemFrom(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        var inputDir = self.getBlockState().getValue(INPUT_FACING);
        if (!conveyor.isContainerEmpty() || pushDirection == inputDir || inputDir.getOpposite() != relative || self.getBlockState().getValue(PART) == Part.TOP) {
            return;
        }

        var be = (FermenterBlockEntity) self.getBlockEntity();

        for (var i : FermenterBlockEntity.OUTPUT_SLOTS) {
            var out = be.getItem(i);

            if (out.isEmpty()) {
                continue;
            }

            var amount = Math.min(out.getCount(), conveyor.getMaxStackCount(out));

            if (out.getCount() == amount) {
                conveyor.pushNew(out);
                be.setItem(i, ItemStack.EMPTY);
                return;
            } else {
                be.setChanged();
                conveyor.setMovementPosition(pushDirection == inputDir.getOpposite() ? 0 : 0.5);
                conveyor.pushNew(out.copyWithCount(amount));

                out.shrink(amount);
                return;
            }
        }
    }


    @Override
    protected BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state) {
        return new FermenterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.FERMENTER ? FermenterBlockEntity::ticker : null;
    }

    @Override
    protected ElementHolder createModel(ServerLevel serverWorld, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(PART) == Part.MAIN;
    }


    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement gears;
        private float rotation;
        private boolean active;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2));
            this.main.setTranslation(new Vector3f(0, 0.5f, 0));
            this.gears = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR_DOUBLE.get(), this.getUpdateRate(), 0.3f, 0.5f);

            this.gears.setViewRange(0.4f);

            this.updateStatePos(state);
            var dir = state.getValue(INPUT_FACING);
            this.updateAnimation(true,  0, (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
            this.addElement(this.main);
            this.addElement(this.gears);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(INPUT_FACING);

            this.main.setYaw(direction.toYRot());
            this.gears.setYaw(direction.toYRot());
        }

        private void updateAnimation(boolean b, float rotation, boolean negative) {
            var mat = mat();
            mat.translate(0, 0.5f, 0);
            if (b) {
                mat.rotateY(negative ? Mth.HALF_PI : -Mth.HALF_PI);
                mat.translate(0, 1 / 8f - 2 / 16f + 0.0001f, 0);
                mat.rotateZ(-rotation);
                this.gears.setTransformation(mat);
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


            var dir = this.blockState().getValue(INPUT_FACING);
            this.updateAnimation(b,
                    b ? RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos().above()).rotation() : 0,
                    (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));

            this.gears.startInterpolationIfDirty();
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
