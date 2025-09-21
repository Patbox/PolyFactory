package eu.pb4.polyfactory.item.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ConfigurationData(List<Entry> entries) implements TooltipAppender  {
    public static final Codec<ConfigurationData> CODEC = Codec.withAlternative(
            Entry.CODEC.listOf(),
            Codec.unboundedMap(Codec.STRING, Codecs.BASIC_OBJECT),
            x -> x.entrySet().stream().map(b -> new Entry(b.getKey(), b.getValue())).collect(Collectors.<Entry>toList())
    ).xmap(ConfigurationData::new, ConfigurationData::entries);
    public static final ConfigurationData EMPTY = new ConfigurationData(List.of());

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    @Override
    public void appendTooltip(Item.TooltipContext context, Consumer<Text> text, TooltipType type, ComponentsAccess components) {
        for (var entry : this.entries) {
            text.accept(Text.literal(" ")
                    .append(entry.name.isPresent() ? entry.name.get() : Text.literal(entry.id))
                    .append(": ")
                    .append(entry.valueText.isPresent() ? entry.valueText.get() : Text.literal(String.valueOf(entry.value)))
                    .formatted(Formatting.GRAY)
            );
        }
    }

    public Map<String, Entry> byId() {
        return this.entries.stream().collect(Collectors.toMap(Entry::id, Function.identity()));
    }

    public record Entry(Optional<Text> name, Optional<Text> valueText, String id, Object value) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TextCodecs.CODEC.optionalFieldOf("name").forGetter(Entry::name),
                TextCodecs.CODEC.optionalFieldOf("value_text").forGetter(Entry::valueText),
                Codec.STRING.fieldOf("id").forGetter(Entry::id),
                Codecs.BASIC_OBJECT.fieldOf("value").forGetter(Entry::value)
        ).apply(instance, Entry::new));

        public Entry(String id, Object value) {
            this(Optional.empty(), Optional.empty(), id, value);
        }

        public Entry(Text name, Text valueText, String id, Object value) {
            this(Optional.ofNullable(name), Optional.ofNullable(valueText), id, value);
        }
    }
}
