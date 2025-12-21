package eu.pb4.polyfactory.block.other;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface FilledStateProvider {
    @Nullable
    Component getFilledStateText();

    long getFilledAmount();
    long getFillCapacity();
}
