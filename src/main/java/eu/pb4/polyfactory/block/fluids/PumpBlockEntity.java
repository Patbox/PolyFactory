package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
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
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PumpBlockEntity extends PipeLikeBlockEntity implements FluidInput.ContainerBased, DebugTextProvider {
    private double speed;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PUMP, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putDouble("speed", this.speed);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.speed = nbt.getDouble("speed");
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PumpBlockEntity pump)) {
            return;
        }
        pump.preTick();

        var speed = RotationUser.getRotation(world, pos).speed();
        pump.speed = speed;
        var strength = speed / 60 / 20;
        NetworkComponent.Pipe.forEachLogic((ServerWorld) world, pos, l -> l.setSourceStrength(pos, strength));

        if (speed != 0) {
            var dir = state.get(PumpBlock.FACING);
            if (pump.container.isNotEmpty()) {
                pump.pushFluid(dir, strength);
            }
            if (pump.container.isNotFull()) {
                pump.pullFluid(dir.getOpposite(), strength);
            }
        }
        pump.postTick();
    }
}
