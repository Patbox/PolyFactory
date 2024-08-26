package eu.pb4.polyfactory.block.other;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface FilledStateProvider {
    @Nullable
    Text getFilledStateText();

    long getFilledAmount();
    long getFillCapacity();
}
