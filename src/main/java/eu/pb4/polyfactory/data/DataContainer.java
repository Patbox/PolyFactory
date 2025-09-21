package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.Consumer;

public interface DataContainer extends Comparable<DataContainer>, TooltipAppender {
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
        return (int) MathHelper.clamp(Math.abs(asLong()), 0, 15);
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
    default void appendTooltip(Item.TooltipContext context, Consumer<Text> textConsumer, TooltipType type, ComponentsAccess components) {
        textConsumer.accept(
                Texts.bracketed(
                        Text.translatable("block.polyfactory.data_memory.tooltip.stored_data").formatted(Formatting.YELLOW)
                ).formatted(Formatting.DARK_GRAY)
        );
        textConsumer.accept(ScreenTexts.space().append(Text.translatable("block.polyfactory.data_memory.tooltip.type",
                Text.translatable("data_type.polyfactory." + this.type().id())).formatted(Formatting.GRAY)));
        textConsumer.accept(ScreenTexts.space().append(Text.translatable("block.polyfactory.data_memory.tooltip.value", this.asString()).formatted(Formatting.GRAY)));
    }
}
