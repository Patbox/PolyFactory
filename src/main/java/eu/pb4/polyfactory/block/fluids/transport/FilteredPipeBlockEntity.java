package eu.pb4.polyfactory.block.fluids.transport;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class FilteredPipeBlockEntity extends PipeLikeBlockEntity implements BlockEntityExtraListener {
    @Nullable
    private FluidInstance<?> allowedFluid = null;
    @Nullable
    private FilteredPipeBlock.Model model;

    public FilteredPipeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FILTERED_PIPE, pos, state);
    }


    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.setAllowedFluid(view.read("allowed_fluid", FluidInstance.CODEC).orElse(null));
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (this.allowedFluid != null) {
            view.store("allowed_fluid", FluidInstance.CODEC, this.allowedFluid);
        }
    }

    @Override
    protected FluidContainerImpl createContainer() {
        return FluidContainerImpl.filtered(FluidConstants.BLOCK, this::checkFluid, this::markFluidDirty);
    }

    private boolean checkFluid(FluidInstance<?> instance) {
        return allowedFluid == null || allowedFluid.equals(instance) == !this.getBlockState().getValue(FilteredPipeBlock.INVERTED);
    }

    private void markFluidDirty() {
        if (this.allowedFluid == null) {
            this.setAllowedFluid(this.container.topFluid());
        }

        this.setChanged();
    }

    public void setAllowedFluid(@Nullable FluidInstance<?> fluid) {
        if (this.allowedFluid == fluid) {
            return;
        }
        this.allowedFluid = fluid;
        if (this.model != null) {
            this.model.setAllowedFluid(fluid);
        }
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof FilteredPipeBlockEntity pipe)) {
            return;
        }
        pipe.preTick();
        if (pipe.container.isNotEmpty()) {
            NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPushFlows(pos, pipe.container::isNotEmpty, pipe::pushFluid);
        }
        if (pipe.container.isNotFull()) {
            NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPullFlows(pos, pipe.container::isNotFull, pipe::pullFluid);
        }
        pipe.postTick();
    }

    @Override
    protected boolean hasDirection(Direction direction) {
        return this.getBlockState().getValue(FilteredPipeBlock.AXIS) == direction.getAxis();
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, worldPosition);
        if (x != null && x.holder() instanceof FilteredPipeBlock.Model model) {
            this.model = model;
            this.model.setAllowedFluid(this.allowedFluid);
        }
    }
}
