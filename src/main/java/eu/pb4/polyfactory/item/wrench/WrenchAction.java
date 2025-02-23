package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public record WrenchAction(String id, Text name, WrenchValueGetter value, WrenchApplyAction action, WrenchApplyAction alt) {
    public static final WrenchAction FACING = ofDirection(Properties.FACING);
    public static final WrenchAction AXIS = of("axis", Properties.AXIS);
    public static final WrenchAction HORIZONTAL_AXIS = of("axis", Properties.HORIZONTAL_AXIS);
    public static final WrenchAction FACING_HORIZONTAL = ofDirection("facing", Properties.HORIZONTAL_FACING );
    public static final WrenchAction HALF = of("half", Properties.BLOCK_HALF, t -> Text.translatable("text.polyfactory.half." + t.asString()));
    public static final WrenchAction CHANNEL = ofChannel("channel", ChannelContainer.class,
        ChannelContainer::channel, ChannelContainer::setChannel
    );

    public static final WrenchAction CHANNEL_WITH_DISABLED = ofChannelWithDisabled("channel", ChannelContainer.class,
            ChannelContainer::channel, ChannelContainer::setChannel
    );
    public static final WrenchAction FACING_HOPPER = ofDirection("rotation", Properties.HOPPER_FACING);
    public static final WrenchAction INVERTED = of("inverted", Properties.INVERTED);

    public WrenchAction(String id, Text name, WrenchValueGetter value, WrenchApplyAction action) {
        this(id, name, value, action, action);
    }

    public WrenchAction withAlt(WrenchApplyAction alt) {
        return new WrenchAction(id, name, value, action, alt);
    }

    public static WrenchAction of(Property<?> property) {
        return of(property.getName(), property);
    }
    public static WrenchAction of(String id, Property<?> property) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofProperty(property), WrenchApplyAction.ofProperty(property));
    }

    public static <T extends Comparable<T>> WrenchAction of(String id, Property<T> property, Function<T, Text> textFunction) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofProperty(property, textFunction), WrenchApplyAction.ofProperty(property));
    }

    public static <T extends Comparable<T>> WrenchAction of(String id, Property<T> property, BiFunction<T, Boolean, T> function) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofProperty(property), WrenchApplyAction.ofProperty(property, function));
    }

    public static <T extends Comparable<T>> WrenchAction of(String id, Property<T> property, Function<T, Text> textFunction, BiFunction<T, Boolean, T> function) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofProperty(property, textFunction), WrenchApplyAction.ofProperty(property, function));
    }

    public static <T extends Comparable<T>> WrenchAction of(String id, WrenchValueGetter value, WrenchApplyAction action) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id), value, action);
    }

    public static <T> WrenchAction ofBlockEntityString(String id, Class<T> tClass, Function<T, String> value, BiConsumer<T, Boolean> change) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofBlockEntity(tClass, value.andThen(Text::literal)), WrenchApplyAction.ofBlockEntity(tClass, change));
    }
    public static <T> WrenchAction ofBlockEntity(String id, Class<T> tClass, Function<T, Text> value, BiConsumer<T, Boolean> change) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofBlockEntity(tClass, value), WrenchApplyAction.ofBlockEntity(tClass, change));
    }

    public static WrenchAction ofDirection(DirectionProperty property) {
        return ofDirection(property.getName(), property);
    }

    public static WrenchAction ofDirection(String id, DirectionProperty property) {
        var reordered = new ArrayList<Direction>();
        for (var x : FactoryUtil.REORDERED_DIRECTIONS) {
            if (property.getValues().contains(x)) {
                reordered.add(x);
            }
        }

        return of(id, property,
                FactoryUtil::asText,
                (dir, next) -> reordered.get(
                        (reordered.size() + reordered.indexOf(dir) + (next ? 1 : -1)) % reordered.size()
                )).withAlt(WrenchApplyAction.ofState((player, world, pos, dir, state, next) -> {
                    dir = next ? dir : dir.getOpposite();
                    return property.getValues().contains(dir) ? state.with(property, dir) : state;
                }));
    }

    public static <T> WrenchAction ofChannel(String id, Class<T> tClass, Function<T, Integer> get, BiConsumer<T, Integer> set) {
        return ofBlockEntityInt(id, tClass, 0, DataStorage.MAX_CHANNELS - 1, 1, get, set);
    }

    public static <T> WrenchAction ofChannelWithDisabled(String id, Class<T> tClass, Function<T, Integer> get, BiConsumer<T, Integer> set) {
        return ofBlockEntityInt(id, tClass, -1, DataStorage.MAX_CHANNELS - 1,
                x -> x != -1 ? String.valueOf(x + 1) : "X", get, set);
    }

    public static <T> WrenchAction ofBlockEntityInt(String id, Class<T> tClass, int minInclusive, int maxInclusive, int displayOffset, Function<T, Integer> get, BiConsumer<T, Integer> set) {
        return ofBlockEntityString(id,
                tClass,
                x -> String.valueOf(get.apply(x) + displayOffset),
                (x, n) -> set.accept(x,  FactoryUtil.wrap(get.apply(x) + (n ? 1 : -1), minInclusive, maxInclusive))
        );
    }

    public static <T> WrenchAction ofBlockEntityInt(String id, Class<T> tClass, int minInclusive, int maxInclusive, IntFunction<String> display, Function<T, Integer> get, BiConsumer<T, Integer> set) {
        return ofBlockEntityString(id,
                tClass,
                x -> display.apply(get.apply(x)),
                (x, n) -> set.accept(x,  FactoryUtil.wrap(get.apply(x) + (n ? 1 : -1), minInclusive, maxInclusive))
        );
    }
}
