package eu.pb4.polyfactory.block.creative;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class CreativeMotorBlockEntity extends LockableBlockEntity {
    private double speed;
    private double stress;

    public CreativeMotorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CREATIVE_MOTOR, pos, state);
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putDouble("Speed", this.speed);
        nbt.putDouble("Stress", this.stress);
    }


    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        this.speed = nbt.getDouble("Speed");
        this.stress = nbt.getDouble("Stress");
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld serverWorld, BlockPos pos) {
        if (this.stress > 0) {
            modifier.provide(this.speed, this.stress, false);
        } else {
            modifier.stress(-this.stress);
        }
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.setTitle(GuiTextures.FILL3.apply(CreativeMotorBlockEntity.this.getName()));
            this.updateNumbers();
            this.setSlot(1, GuiTextures.MINUS_BUTTON.get().unbreakable().setCallback((clickType) -> {
                CreativeMotorBlockEntity.this.speed = Math.max(CreativeMotorBlockEntity.this.speed - (clickType.shift ? 10 : 1), 0);
                CreativeMotorBlockEntity.this.markDirty();
            }));
            this.setSlot(7, GuiTextures.PLUS_BUTTON.get().unbreakable().setCallback((clickType) -> {
                CreativeMotorBlockEntity.this.speed = Math.min(CreativeMotorBlockEntity.this.speed + (clickType.shift ? 10 : 1), RotationConstants.MAX_SPEED);
                CreativeMotorBlockEntity.this.markDirty();
            }));

            this.setSlot(1 + 9 * 2, GuiTextures.MINUS_BUTTON.get().setCallback((clickType) -> {
                CreativeMotorBlockEntity.this.stress = Math.max(CreativeMotorBlockEntity.this.stress - (clickType.shift ? 10 : 1), -1000);
                CreativeMotorBlockEntity.this.markDirty();
            }));
            this.setSlot(7 + 9 * 2, GuiTextures.PLUS_BUTTON.get().setCallback((clickType) -> {
                CreativeMotorBlockEntity.this.stress = Math.min(CreativeMotorBlockEntity.this.stress + (clickType.shift ? 10 : 1), 99999);
                CreativeMotorBlockEntity.this.markDirty();
            }));
            this.open();
        }

        private void updateNumbers() {
            GuiUtils.drawFlatNumbers(this, 2, (int) CreativeMotorBlockEntity.this.speed, 5, Formatting.DARK_GRAY.getColorValue(), false);
            GuiUtils.drawFlatNumbers(this, 2 + 9 * 2, (int) CreativeMotorBlockEntity.this.stress, 5, CreativeMotorBlockEntity.this.stress < 0 ? Formatting.DARK_RED.getColorValue() : Formatting.DARK_GRAY.getColorValue(), false);
        }

        @Override
        public void onTick() {
            updateNumbers();
            super.onTick();
        }
    }
}
