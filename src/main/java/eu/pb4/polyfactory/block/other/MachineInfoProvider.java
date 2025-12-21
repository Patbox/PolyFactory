package eu.pb4.polyfactory.block.other;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface MachineInfoProvider {
    Component OVERSTRESSED_TEXT = Component.translatable("text.polyfactory.state.overstressed").withStyle(ChatFormatting.RED);
    Component LOCKED_TEXT = Component.translatable("text.polyfactory.state.locked").withStyle(ChatFormatting.RED);
    Component TOO_SLOW_TEXT = Component.translatable("text.polyfactory.state.too_slow").withStyle(ChatFormatting.YELLOW);
    Component TOO_HOT_TEXT = Component.translatable("text.polyfactory.state.too_hot").withStyle(ChatFormatting.YELLOW);
    Component TOO_COLD_TEXT = Component.translatable("text.polyfactory.state.too_cold").withStyle(ChatFormatting.YELLOW);
    Component INCORRECT_ITEMS_TEXT = Component.translatable("text.polyfactory.state.incorrect_items").withStyle(ChatFormatting.YELLOW);
    Component OUTPUT_FULL_TEXT = Component.translatable("text.polyfactory.state.output_full").withStyle(ChatFormatting.YELLOW);
    @Nullable Component getCurrentState();
}
