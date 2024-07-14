package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.util.DebugTextProvider;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class DrainBlockEntity extends BlockEntity implements FluidInputOutput.ContainerBased, BlockEntityExtraListener {
    public static final long CAPACITY = FluidConstants.BLOCK;
    private final FluidContainer container = new FluidContainer(CAPACITY, this::onFluidChanged);

    @Nullable
    private DrainBlock.Model model;

    public DrainBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.DRAIN, pos, state);
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("fluid", this.container.toNbt(registryLookup));
        updateModel();
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.container.fromNbt(registryLookup, nbt, "fluid");
    }

    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.container;
    }

    private void onFluidChanged() {
        this.markDirty();
        this.updateModel();
    }

    private void updateModel() {
        if (this.model != null) {
            this.model.setFluid(this.container.topFluid(), this.container.getFilledPercentage());
        }
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, pos);
        if (x != null && x.holder() instanceof DrainBlock.Model model) {
            this.model = model;
        }
    }
}
