package eu.pb4.polyfactory.block.creative;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.scores.TeamColor;

public class CreativeMotorBlockEntity extends LockableBlockEntity {
    private double speed;
    private double stress;

    public CreativeMotorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CREATIVE_MOTOR, pos, state);
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putDouble("Speed", this.speed);
        view.putDouble("Stress", this.stress);
    }


    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.speed = view.getDoubleOr("Speed", 0);
        this.stress = view.getDoubleOr("Stress", 0);
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel serverWorld, BlockPos pos) {
        if (this.stress > 0) {
            modifier.provide(this.speed, this.stress, false);
        } else {
            modifier.stress(-this.stress);
        }
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.setTitle(GuiTextures.FILL3.apply(CreativeMotorBlockEntity.this.getName()));
            this.updateNumbers();
            this.setSlot(1, GuiTextures.MINUS_BUTTON.get().hideTooltip().unbreakable().setCallback((clickType) -> {
                setRPM(Math.max(getRPM() - (clickType.shift ? 10 : 1), -Math.round(FactoryUtil.degreesPerTickToRPM(RotationConstants.MAX_SPEED * 50))));
                CreativeMotorBlockEntity.this.setChanged();
            }));
            this.setSlot(7, GuiTextures.PLUS_BUTTON.get().hideTooltip().unbreakable().setCallback((clickType) -> {
                setRPM(Math.min(getRPM() + (clickType.shift ? 10 : 1), Math.round(FactoryUtil.degreesPerTickToRPM(RotationConstants.MAX_SPEED * 50))));
                CreativeMotorBlockEntity.this.setChanged();
            }));

            this.setSlot(1 + 9 * 2, GuiTextures.MINUS_BUTTON.get().hideTooltip().setCallback((clickType) -> {
                CreativeMotorBlockEntity.this.stress = Math.max(CreativeMotorBlockEntity.this.stress - (clickType.shift ? 10 : 1), -1000);
                CreativeMotorBlockEntity.this.setChanged();
            }));
            this.setSlot(7 + 9 * 2, GuiTextures.PLUS_BUTTON.get().hideTooltip().setCallback((clickType) -> {
                CreativeMotorBlockEntity.this.stress = Math.min(CreativeMotorBlockEntity.this.stress + (clickType.shift ? 10 : 1), 99999);
                CreativeMotorBlockEntity.this.setChanged();
            }));
            this.open();
        }

        private void updateNumbers() {
            GuiUtils.drawFlatNumbers(this, 2, (int) CreativeMotorBlockEntity.this.speed, 5, TextColor.DARK_GRAY.getValue(), false);
            GuiUtils.drawFlatNumbers(this, 2 + 9 * 2, (int) CreativeMotorBlockEntity.this.stress, 5, CreativeMotorBlockEntity.this.stress < 0 ? TextColor.DARK_RED.getValue() : TextColor.DARK_GRAY.getValue(), false);
        }

        @Override
        public void onTick() {
            updateNumbers();
            super.onTick();
        }
    }

    private double getRPM() {
        return FactoryUtil.degreesPerTickToRPM(this.speed);
    }

    private void setRPM(double speed) {
        this.speed = FactoryUtil.rpmToDegreesPerTick(speed);
        this.setChanged();
    }
}
