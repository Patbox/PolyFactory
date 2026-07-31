package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.item.component.FluidComponent;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public interface FluidContainer extends FluidHolder, FluidExchangeHandler {
    FluidContainer EMPTY = new FluidContainerImpl(0, () -> {}, (_, _) -> false);

    long set(FluidInstance<?> type, long amount);

    void clear();

    default int updateId() {
        return System.identityHashCode(this);
    }

    FluidComponent asFluidComponent();
}
