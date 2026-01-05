package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SimpleInputGui extends AnvilInputGui {
    private final BooleanSupplier shouldClose;
    private final Predicate<String> isValid;
    private final Consumer<String> consumer;

    public SimpleInputGui(ServerPlayer player, Component title, BooleanSupplier shouldClose, String defaultValue,
                          Predicate<String> isValid, Consumer<String> consumer) {
        super(player, false);
        this.shouldClose = shouldClose;
        this.isValid = isValid;
        this.consumer = consumer;
        this.setTitle(GuiTextures.INPUT.apply(Component.empty().append(
                Component.literal(("" + GuiTextures.BLUEPRINT_WORKSTATION_EXTRA_OFFSET).repeat(2)).setStyle(UiResourceCreator.STYLE)
        ).append(title)));
        this.updateDone();
        this.setSlot(2, GuiTextures.BUTTON_CLOSE.get().setName(CommonComponents.GUI_BACK).setCallback(x -> {
            FactoryUtil.playSoundToPlayer(player, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
            this.close();
        }));
        this.setDefaultInputValue(defaultValue);
        this.open();
    }

    @Override
    public void onInput(String input) {
        super.onInput(input);
        this.updateDone();
        if (this.screenHandler != null) {
            this.screenHandler.setRemoteSlot(2, ItemStack.EMPTY);
        }
    }

    private void updateDone() {
        if (this.isValid.test(this.getInput())) {
            this.setSlot(1, GuiTextures.BUTTON_DONE.get().setName(CommonComponents.GUI_DONE).setCallback(x -> {
                FactoryUtil.playSoundToPlayer(player, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
                this.consumer.accept(this.getInput());
                this.close();
            }));
        } else {
            this.setSlot(1, GuiTextures.BUTTON_DONE_BLOCKED.get().setName(Component.empty().append(CommonComponents.GUI_DONE).withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void setDefaultInputValue(String input) {
        super.setDefaultInputValue(input);
        if (this.consumer != null) {
            updateDone();
        }
        var itemStack = GuiTextures.EMPTY.getItemStack().copy();
        itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(input));
        itemStack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
        this.setSlot(0, itemStack, Objects.requireNonNull(this.getSlot(0)).getGuiCallback());
    }

    @Override
    public void onTick() {
        if (shouldClose.getAsBoolean()) {
            this.close();
            return;
        }
        super.onTick();
    }
}