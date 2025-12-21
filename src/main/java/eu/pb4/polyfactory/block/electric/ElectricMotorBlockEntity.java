package eu.pb4.polyfactory.block.electric;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ElectricMotorBlockEntity extends LockableBlockEntity {
    private float speed = 10;
    private float stress = 10;

    public ElectricMotorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.ELECTRIC_MOTOR, pos, state);
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        //new Gui(playerEntity);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putFloat("speed", this.speed);
        view.putFloat("stress", this.stress);
    }


    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.speed = view.getFloatOr("speed", 0);
        this.stress = view.getFloatOr("stress", 0);
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel serverWorld, BlockPos pos) {
        var energy = EnergyUser.getEnergy(serverWorld, pos);

        if (energy.current() > 0) {
            modifier.provide(this.speed, this.stress, false);
        }
    }

    public void updateEnergyData(EnergyData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        modifier.use((long) (this.speed * (this.stress * 1.2f)));
    }


}
