package eu.pb4.polyfactory.block.electric;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

public class ElectricMotorBlockEntity extends LockableBlockEntity {
    private float speed = 10;
    private float stress = 10;

    public ElectricMotorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.ELECTRIC_MOTOR, pos, state);
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        //new Gui(playerEntity);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putFloat("speed", this.speed);
        view.putFloat("stress", this.stress);
    }


    @Override
    public void readData(ReadView view) {
        super.readData(view);
        this.speed = view.getFloat("speed", 0);
        this.stress = view.getFloat("stress", 0);
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld serverWorld, BlockPos pos) {
        var energy = EnergyUser.getEnergy(serverWorld, pos);

        if (energy.current() > 0) {
            modifier.provide(this.speed, this.stress, false);
        }
    }

    public void updateEnergyData(EnergyData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        modifier.use((long) (this.speed * (this.stress * 1.2f)));
    }


}
