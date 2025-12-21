package eu.pb4.polyfactory.item.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public record ConfigurationData(List<Entry> entries) implements TooltipProvider  {
    public static final Codec<ConfigurationData> CODEC = Codec.withAlternative(
            Entry.CODEC.listOf(),
            Codec.unboundedMap(Codec.STRING, ExtraCodecs.JAVA),
            x -> x.entrySet().stream().map(b -> new Entry(b.getKey(), b.getValue())).collect(Collectors.<Entry>toList())
    ).xmap(ConfigurationData::new, ConfigurationData::entries);
    public static final ConfigurationData EMPTY = new ConfigurationData(List.of());

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> text, TooltipFlag type, DataComponentGetter components) {
        for (var entry : this.entries) {
            text.accept(Component.literal(" ")
                    .append(entry.name.isPresent() ? entry.name.get() : Component.literal(entry.id))
                    .append(": ")
                    .append(entry.valueText.isPresent() ? entry.valueText.get() : Component.literal(String.valueOf(entry.value)))
                    .withStyle(ChatFormatting.GRAY)
            );
        }
    }

    public Map<String, Entry> byId() {
        return this.entries.stream().collect(Collectors.toMap(Entry::id, Function.identity()));
    }

    public record Entry(Optional<Component> name, Optional<Component> valueText, String id, Object value) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Entry::name),
                ComponentSerialization.CODEC.optionalFieldOf("value_text").forGetter(Entry::valueText),
                Codec.STRING.fieldOf("id").forGetter(Entry::id),
                ExtraCodecs.JAVA.fieldOf("value").forGetter(Entry::value)
        ).apply(instance, Entry::new));

        public Entry(String id, Object value) {
            this(Optional.empty(), Optional.empty(), id, value);
        }

        public Entry(Component name, Component valueText, String id, Object value) {
            this(Optional.ofNullable(name), Optional.ofNullable(valueText), id, value);
        }
    }
}
