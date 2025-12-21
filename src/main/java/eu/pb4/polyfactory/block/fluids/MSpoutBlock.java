package eu.pb4.polyfactory.block.fluids;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.GenericParts;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.pipe.PumpNode;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public class MSpoutBlock extends TallItemMachineBlock implements NetworkComponent.Pipe, PipeConnectable {
    public MSpoutBlock(Properties settings) {
        super(settings);
    }


    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof Container inventory) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(inventory);
        }
        return 0;
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        if (self.getBlockState().getValue(INPUT_FACING) == pushDirection || self.getBlockState().getValue(PART) == Part.TOP) {
            return false;
        }

        var be = (MSpoutBlockEntity) self.getBlockEntity();

        var container = be.getContainerHolder(MSpoutBlockEntity.INPUT_FIRST);

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

        var be = (MSpoutBlockEntity) self.getBlockEntity();

        var out = be.getContainerHolder(MSpoutBlockEntity.OUTPUT_FIRST);

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
        return world instanceof ServerLevel && type == FactoryBlockEntities.MECHANICAL_SPOUT ? MSpoutBlockEntity::ticker : null;
    }

    @Override
    protected BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state) {
        return new MSpoutBlockEntity(pos, state);
    }

    @Override
    protected ElementHolder createModel(ServerLevel serverWorld, BlockPos pos, BlockState initialBlockState) {
        return new Model(serverWorld, initialBlockState);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.ANVIL.defaultBlockState();
    }

    @Override
    public Collection<BlockNode> createPipeNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return state.getValue(PART) == Part.TOP ? List.of(new PumpNode(Direction.DOWN, true, PumpNode.SPOUT_RANGE)) : List.of();
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        NetworkComponent.Rotational.updateRotationalAt(world, pos);
        NetworkComponent.Pipe.updatePipeAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Rotational || block instanceof NetworkComponent.Pipe;
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return dir == Direction.UP;
    }

    public static final class Model extends RotationAwareModel {
        private final ItemStack DEFAULT_MODEL = ItemDisplayElementUtil.getModel(id("block/mechanical_spout"));
        private final ItemStack ALT_MODEL = ItemDisplayElementUtil.getModel(id("block/mechanical_spout_alt"));
        private final ItemDisplayElement main;
        private final ItemDisplayElement gearA;
        private final ItemDisplayElement gearB;
        private final ItemDisplayElement fluid;
        private boolean altModel = false;
        private double progress;
        private FluidInstance<?> castingFluid;
        private boolean isCooling = false;

        private Model(ServerLevel world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(DEFAULT_MODEL);
            this.main.setScale(new Vector3f(2));
            this.main.setTranslation(new Vector3f(0, 0.5f, 0));
            this.gearA = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR, this.getUpdateRate(), 0.3f, 0.5f);
            this.gearB = LodItemDisplayElement.createSimple(GenericParts.SMALL_GEAR, this.getUpdateRate(), 0.3f, 0.5f);
            this.fluid = LodItemDisplayElement.createSimple();
            this.gearA.setViewRange(0.4f);
            this.gearB.setViewRange(0.4f);
            this.fluid.setViewRange(0.4f);
            this.fluid.setScale(new Vector3f(12 / 16f));
            this.fluid.setOffset(new Vec3(0, 7.5 / 16f - (2 / 16f / 16f), 0));

            updateStatePos(state);
            var dir = state.getValue(INPUT_FACING);
            this.updateAnimation(0, (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
            this.addElement(this.main);
            this.addElement(this.fluid);
            this.addElement(this.gearA);
            this.addElement(this.gearB);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(INPUT_FACING);

            this.main.setYaw(direction.toYRot());
            this.fluid.setYaw(direction.toYRot());
            this.gearA.setYaw(direction.toYRot());
            this.gearB.setYaw(direction.toYRot());
        }

        private void updateAnimation(float rotation, boolean negative) {
            var mat = mat();
            mat.identity();
            mat.translate(0, 0.5f, 0);
            mat.rotateY(negative ? Mth.HALF_PI : -Mth.HALF_PI);
            mat.translate(0, 0.5f, 0.40f);
            mat.rotateZ(rotation);
            this.gearA.setTransformation(mat);
            mat.translate(0, 0, -0.80f);
            this.gearB.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getTick();
            var b = tick % this.getUpdateRate() == 0;
            var dir = this.blockState().getValue(INPUT_FACING);
            if (b) {
                this.updateAnimation(RotationUser.getRotation(this.getAttachment().getWorld(), this.blockPos().above()).rotation(),
                        (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.X));
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

        public void setActive(boolean b) {
            if (!b) {
                setProgress(false, 0, null);
            }
        }

        public void altModel(boolean b) {
            if (this.altModel == b) {
                return;
            }
            if (!b) {
                setProgress(false, 0, null);
            }
            this.altModel = b;
            this.main.setItem(b ? ALT_MODEL : DEFAULT_MODEL);
            this.main.tick();
        }

        public void setProgress(boolean isCooling, double process, FluidInstance<?> castingFluid) {
            if (!this.altModel) {
                return;
            }
            if (this.progress == process && this.castingFluid == castingFluid && this.isCooling == isCooling) {
                return;
            }

            if (castingFluid == null) {
                this.fluid.setItem(ItemStack.EMPTY);
                this.fluid.setTranslation(new Vector3f(0, -0.1f, 0));
            } else {
                var isSolid = process > 0.5 && isCooling;
                var value = Mth.clamp((float) (isSolid ? ((process - 0.50f) / 0.50f) : (1 - (process) / 0.50f)), 0, 1);
                var color = isCooling ? ARGB.colorFromFloat(1, 1f, 0.6f + value * 0.4f, 0.5f + value * 0.5f) : 0xFFFFFF;
                this.fluid.setItem(FactoryModels.FLUID_FLAT_14_SPOUT.get(castingFluid,
                        color,
                        isSolid));
                this.fluid.setTranslation(new Vector3f( 0, (float) ((isCooling ? 1 : Mth.clamp(process, 0, 1)) - 0.5) / 16.2f * 12 / 16f, 0));
            }
            if (process > this.progress && !isCooling) {
                this.fluid.startInterpolationIfDirty();
            }

            this.fluid.tick();
            this.progress = process;
            this.castingFluid = castingFluid;
            this.isCooling = isCooling;
        }
    }
}
