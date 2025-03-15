package eu.pb4.polyfactory.item.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public record ClipboardData(List<Entry> entries) {
    public static final Codec<ClipboardData> CODEC = Codec.withAlternative(
            Entry.CODEC.listOf(),
            Codec.unboundedMap(Codec.STRING, Codecs.BASIC_OBJECT),
            x -> x.entrySet().stream().map(b -> new Entry(b.getKey(), b.getValue())).collect(Collectors.<Entry>toList())
    ).xmap(ClipboardData::new, ClipboardData::entries);
    public static final ClipboardData EMPTY = new ClipboardData(List.of());

    public void addToTooltip(Consumer<Text> text) {
        for (var entry : this.entries) {
            text.accept(Text.literal(" ")
                    .append(entry.name.isPresent() ? entry.name.get() : Text.literal(entry.id))
                    .append(": ")
                    .append(entry.valueText.isPresent() ? entry.valueText.get() : Text.literal(String.valueOf(entry.value)))
                    .formatted(Formatting.GRAY)
            );
        }
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
