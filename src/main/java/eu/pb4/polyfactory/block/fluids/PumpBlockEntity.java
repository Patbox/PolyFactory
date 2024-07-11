package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.util.DebugTextProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PumpBlockEntity extends BlockEntity implements FluidInput.ContainerBased, DebugTextProvider {
    public static final long CAPACITY = PipeBlockEntity.CAPACITY;
    private final FluidContainer container = FluidContainer.singleFluid(CAPACITY, this::markDirty);
    private long overflow = 0;
    private double speed;
    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PUMP, pos, state);
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("fluid", this.container.toNbt());
        nbt.putLong("overflow", this.overflow);
        nbt.putDouble("speed", this.speed);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.container.fromNbt(nbt.getCompound("fluid"));
        this.overflow = nbt.getLong("overflow");
        this.speed = nbt.getDouble("speed");
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PumpBlockEntity pump)) {
            return;
        }

        var speed = RotationUser.getRotation(world, pos).speed();
        pump.speed = speed;
        NetworkComponent.Pipe.forEachLogic((ServerWorld) world, pos, l -> l.setSourceStrength(pos, speed / 60));
        //NetworkComponent.Pipe.forEachLogic((ServerWorld) world, pos, l -> l.setSourceStrength(pos, 1));

        if (speed == 0) {
            return;
        }

        if (pump.container.isEmpty()) {
            return;
        }

        var amount = Math.min((long) (speed / 60 * FluidConstants.BOTTLE), FluidConstants.BOTTLE);
        var dir = state.get(PumpBlock.FACING);
        var next = world.getBlockEntity(pos.offset(dir));
        if (next instanceof FluidInput inputOutput) {
            var fluid = pump.container.topFluid();
            amount = inputOutput.insertFluid(fluid, pump.container.extract(fluid, amount, false), dir.getOpposite());
            if (amount != 0) {
                pump.container.insert(fluid, amount, false);
            }
        }
    }
    @Override
    public Text getDebugText() {
        return Text.literal("F: " + this.container.getFilledPercentage());
    }
}
