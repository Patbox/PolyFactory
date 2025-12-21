package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public interface DataContainer extends Comparable<DataContainer>, TooltipProvider {
    MapCodec<DataContainer> MAP_CODEC = MapCodec.recursive("data_container", x -> DataType.CODEC.dispatchMap("type", DataContainer::type, DataType::codec));
    Codec<DataContainer> CODEC = MAP_CODEC.codec();

    List<String> GENERIC_EXTRACTS = List.of("decimal", "integer", "string", "boolean", "redstone", "progress");
    static DataContainer of(long count) {
        return new LongData(count);
    }
    static DataContainer of(boolean count) {
        return count ? BoolData.TRUE : BoolData.FALSE;
    }
    static DataContainer empty() {
        return EmptyData.INSTANCE;
    }

    DataType<? extends DataContainer> type();

    String asString();
    long asLong();
    double asDouble();

    default float asProgress() {
        return (float) (asDouble() / 100);
    }
    default boolean isEmpty() {
        return false;
    }

    default int asRedstoneOutput() {
        return (int) Mth.clamp(Math.abs(asLong()), 0, 15);
    };
    default char padding() {
        return ' ';
    }
    default boolean forceRight() {
        return false;
    }

    default DataContainer extract(String field) {
        return switch (field) {
            case "decimal" -> new DoubleData(asDouble());
            case "integer" -> new LongData(asLong());
            case "redstone" -> new RedstoneData(asRedstoneOutput());
            case "string" -> new StringData(asString());
            case "boolean" -> BoolData.of(this.isTrue());
            case "progress" -> new ProgressData(this.asProgress());
            case "" -> this;
            default -> empty();
        };
    }

    default boolean isTrue() {
        return this.asLong() != 0;
    }

    default int compareTo(DataContainer other) {
        return Long.compare(this.asLong(), other.asLong());
    }


    @Override
    default void addToTooltip(Item.TooltipContext context, Consumer<Component> textConsumer, TooltipFlag type, DataComponentGetter components) {
        textConsumer.accept(
                ComponentUtils.wrapInSquareBrackets(
                        Component.translatable("block.polyfactory.data_memory.tooltip.stored_data").withStyle(ChatFormatting.YELLOW)
                ).withStyle(ChatFormatting.DARK_GRAY)
        );
        textConsumer.accept(CommonComponents.space().append(Component.translatable("block.polyfactory.data_memory.tooltip.type",
                Component.translatable("data_type.polyfactory." + this.type().id())).withStyle(ChatFormatting.GRAY)));
        textConsumer.accept(CommonComponents.space().append(Component.translatable("block.polyfactory.data_memory.tooltip.value", this.asString()).withStyle(ChatFormatting.GRAY)));
    }
}
