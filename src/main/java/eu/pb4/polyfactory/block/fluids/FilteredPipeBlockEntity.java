package eu.pb4.polyfactory.block.fluids;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
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
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("allowed_fluid")) {
            this.setAllowedFluid(FluidInstance.CODEC.decode(registryLookup.getOps(NbtOps.INSTANCE), nbt.getCompound("allowed_fluid"))
                    .result().map(Pair::getFirst).orElse(null));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (this.allowedFluid != null) {
            nbt.put("allowed_fluid", FluidInstance.CODEC.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), this.allowedFluid).getOrThrow());
        }
    }

    @Override
    protected FluidContainerImpl createContainer() {
        return FluidContainerImpl.filteredSingleFluid(FluidConstants.BLOCK, this::checkFluid, this::markFluidDirty);
    }

    private boolean checkFluid(FluidInstance<?> instance) {
        return allowedFluid == null || allowedFluid.equals(instance) == !this.getCachedState().get(FilteredPipeBlock.NEGATED);
    }

    private void markFluidDirty() {
        if (this.allowedFluid == null) {
            this.setAllowedFluid(this.container.topFluid());
        }

        this.markDirty();
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

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof FilteredPipeBlockEntity pipe)) {
            return;
        }
        pipe.preTick();
        if (pipe.container.isNotEmpty()) {
            NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPushFlows(pos, pipe.container::isNotEmpty, pipe::pushFluid);
        }
        if (pipe.container.isNotFull()) {
            NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPullFlows(pos, pipe.container::isNotFull, pipe::pullFluid);
        }
        pipe.postTick();
    }

    @Override
    protected boolean hasDirection(Direction direction) {
        return this.getCachedState().get(FilteredPipeBlock.AXIS) == direction.getAxis();
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, pos);
        if (x != null && x.holder() instanceof FilteredPipeBlock.Model model) {
            this.model = model;
            this.model.setAllowedFluid(this.allowedFluid);
        }
    }
}
