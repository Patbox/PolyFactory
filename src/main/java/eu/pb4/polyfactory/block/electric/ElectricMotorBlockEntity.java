package eu.pb4.polyfactory.block.electric;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.base.LockableBlockEntity;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putFloat("speed", this.speed);
        nbt.putFloat("stress", this.stress);
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.speed = nbt.getFloat("speed");
        this.stress = nbt.getFloat("stress");
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


    /*private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.setTitle(GuiTextures.FILL3.apply(ElectricMotorBlockEntity.this.getName()));
            this.updateNumbers();
            this.setSlot(1, GuiTextures.MINUS_BUTTON.get().unbreakable().setCallback((clickType) -> {
                ElectricMotorBlockEntity.this.speed = Math.max(ElectricMotorBlockEntity.this.speed - (clickType.shift ? 10 : 1), 0);
                ElectricMotorBlockEntity.this.markDirty();
            }));
            this.setSlot(7, GuiTextures.PLUS_BUTTON.get().unbreakable().setCallback((clickType) -> {
                ElectricMotorBlockEntity.this.speed = Math.min(ElectricMotorBlockEntity.this.speed + (clickType.shift ? 10 : 1), RotationConstants.MAX_SPEED);
                ElectricMotorBlockEntity.this.markDirty();
            }));

            this.setSlot(1 + 9 * 2, GuiTextures.MINUS_BUTTON.get().setCallback((clickType) -> {
                ElectricMotorBlockEntity.this.stress = Math.max(ElectricMotorBlockEntity.this.stress - (clickType.shift ? 10 : 1), -1000);
                ElectricMotorBlockEntity.this.markDirty();
            }));
            this.setSlot(7 + 9 * 2, GuiTextures.PLUS_BUTTON.get().setCallback((clickType) -> {
                ElectricMotorBlockEntity.this.stress = Math.min(ElectricMotorBlockEntity.this.stress + (clickType.shift ? 10 : 1), 99999);
                ElectricMotorBlockEntity.this.markDirty();
            }));
            this.open();
        }

        private void updateNumbers() {
            GuiUtils.drawFlatNumbers(this, 2, (int) ElectricMotorBlockEntity.this.speed, 5, Formatting.DARK_GRAY.getColorValue(), false);
            GuiUtils.drawFlatNumbers(this, 2 + 9 * 2, (int) ElectricMotorBlockEntity.this.stress, 5, ElectricMotorBlockEntity.this.stress < 0 ? Formatting.DARK_RED.getColorValue() : Formatting.DARK_GRAY.getColorValue(), false);
        }

        @Override
        public void onTick() {
            updateNumbers();
            super.onTick();
        }
    }*/
}
