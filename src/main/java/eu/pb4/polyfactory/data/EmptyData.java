package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public record EmptyData() implements DataContainer {
    public static final EmptyData INSTANCE = new EmptyData();
    public static MapCodec<EmptyData> TYPE_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public DataType<EmptyData> type() {
        return DataType.EMPTY;
    }

    @Override
    public String asString() {
        return "";
    }

    @Override
    public long asLong() {
        return 0;
    }

    @Override
    public double asDouble() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int compareTo(@NotNull DataContainer o) {
        return o.isEmpty() ? 0 : -1;
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> textConsumer, TooltipFlag type, DataComponentGetter components) {
        // No tooltip!
    }
}