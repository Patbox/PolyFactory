package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public record WrenchAction(String id, Text name, WrenchValueGetter value, WrenchApplyAction action, WrenchApplyAction alt) {
    public static final WrenchAction FACING = ofDirection(Properties.FACING);
    public static final WrenchAction AXIS = of("axis", Properties.AXIS);
    public static final WrenchAction FACING_HORIZONTAL = ofDirection("facing", Properties.HORIZONTAL_FACING);
    public static final WrenchAction HALF = of("half", Properties.BLOCK_HALF);
    public static final WrenchAction CHANNEL = ofChannel("channel", ChannelContainer.class,
        ChannelContainer::channel, ChannelContainer::setChannel
    );
    public static final WrenchAction FACING_HOPPER = ofDirection("rotation", Properties.HOPPER_FACING);

    public WrenchAction(String id, Text name, WrenchValueGetter value, WrenchApplyAction action) {
        this(id, name, value, action, action);
    }

    public WrenchAction withAlt(WrenchApplyAction alt) {
        return new WrenchAction(id, name, value, action, alt);
    }

    public static WrenchAction of(String id, Property<?> property) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofProperty(property), WrenchApplyAction.ofProperty(property));
    }

    public static <T extends Comparable<T>> WrenchAction of(String id, Property<T> property, BiFunction<T, Boolean, T> function) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofProperty(property), WrenchApplyAction.ofProperty(property, function));
    }

    public static <T extends Comparable<T>> WrenchAction of(String id, WrenchValueGetter value, WrenchApplyAction action) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id), value, action);
    }

    public static <T> WrenchAction ofBlockEntity(String id, Class<T> tClass, Function<T, String> value, BiConsumer<T, Boolean> change) {
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
                (dir, next) -> reordered.get(
                        (reordered.size() + reordered.indexOf(dir) + (next ? 1 : -1)) % reordered.size()
                )).withAlt(WrenchApplyAction.ofState((player, world, pos, dir, state, next) -> {
                    dir = next ? dir : dir.getOpposite();
                    return property.getValues().contains(dir) ? state.with(property, dir) : state;
                }));
    }

    public static <T> WrenchAction ofChannel(String id, Class<T> tClass, Function<T, Integer> get, BiConsumer<T, Integer> set) {
        return  ofBlockEntity(id,
                tClass,
                x -> String.valueOf(get.apply(x) + 1),
                (x, n) -> set.accept(x, (get.apply(x) + DataStorage.MAX_CHANNELS + (n ? 1 : -1)) % DataStorage.MAX_CHANNELS)
        );
    }
}
