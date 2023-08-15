package eu.pb4.polyfactory.block.electric;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.other.LockableBlockEntity;
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
    long storedPower;

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
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld serverWorld, BlockPos pos) {
        if (!state.get(ElectricMotorBlock.GENERATOR)) {
            if (this.storedPower > 0) {
                modifier.provide(10, 8);
                this.storedPower -= 100;
            }
        } else {
            modifier.stress(10);
            EnergyUser.getEnergy(serverWorld, pos);
        }
    }

    public void updateEnergyData(EnergyData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (state.get(ElectricMotorBlock.GENERATOR)) {
            if (this.storedPower > 0) {
                modifier.provide(110);
                this.storedPower -= 110;
            }
        } else {
            modifier.use(100);
        }
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof ElectricMotorBlockEntity be) || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (state.get(ElectricMotorBlock.GENERATOR)) {
            var rot = RotationUser.getRotation(serverWorld, pos);
            if (rot.speed() > 0) {
                be.storedPower = Math.min(be.storedPower + 110, 300);
            }
        } else {
            var rot = EnergyUser.getEnergy(serverWorld, pos);
            if (rot.current() > 0) {
                be.storedPower = Math.min(be.storedPower + 110, 300);
            }
        }
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
