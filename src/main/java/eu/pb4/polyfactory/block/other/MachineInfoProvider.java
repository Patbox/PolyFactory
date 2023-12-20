package eu.pb4.polyfactory.block.other;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public interface MachineInfoProvider {
    Text OVERSTRESSED_TEXT = Text.translatable("text.polyfactory.state.overstressed").formatted(Formatting.RED);
    Text LOCKED_TEXT = Text.translatable("text.polyfactory.state.locked").formatted(Formatting.RED);
    Text TOO_SLOW_TEXT = Text.translatable("text.polyfactory.state.too_slow").formatted(Formatting.YELLOW);
    Text TOO_HOT_TEXT = Text.translatable("text.polyfactory.state.too_hot").formatted(Formatting.YELLOW);
    Text TOO_COLD_TEXT = Text.translatable("text.polyfactory.state.too_cold").formatted(Formatting.YELLOW);
    Text INCORRECT_ITEMS_TEXT = Text.translatable("text.polyfactory.state.incorrect_items").formatted(Formatting.YELLOW);
    Text OUTPUT_FULL_TEXT = Text.translatable("text.polyfactory.state.output_full").formatted(Formatting.YELLOW);
    @Nullable Text getCurrentState();
}
