package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.util.DebugTextProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PipeBlockEntity extends BlockEntity implements FluidInput.ContainerBased, DebugTextProvider {
    public static final long CAPACITY = FluidConstants.BLOCK;
    private final FluidContainer container = FluidContainer.singleFluid(CAPACITY, this::markDirty);
    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PIPE, pos, state);
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("fluid", this.container.toNbt());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.container.fromNbt(nbt.getCompound("fluid"));
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PipeBlockEntity pipe)) {
            return;
        }
        if (pipe.container.isEmpty()) {
            return;
        }
        // Todo fix being able to pull multiple times within a tick, also fluid duping??
        NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runFlows(pos, pipe.container::isNotEmpty, ((direction, strength) -> {
            var amount = Math.min((long) (strength * FluidConstants.BOTTLE), FluidConstants.BOTTLE);
            var next = world.getBlockEntity(pos.offset(direction));
            if (next instanceof FluidInput inputOutput) {
                var fluid = pipe.container.topFluid();
                amount = inputOutput.insertFluid(fluid, pipe.container.extract(fluid, amount, false), direction.getOpposite());
                if (amount != 0) {
                    pipe.container.insert(fluid, amount, false);
                } else {
                    //((ServerWorld) world).spawnParticles(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0, 0, 0, 0, 0);
                }
            }
        }));
    }
    @Override
    public Text getDebugText() {
        return Text.literal("F: " + this.container.getFilledPercentage());
    }
}
