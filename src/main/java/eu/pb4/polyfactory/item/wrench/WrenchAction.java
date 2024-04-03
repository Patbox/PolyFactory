package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public record WrenchAction(String id, Text name, WrenchValueGetter value, WrenchApplyAction action) {
    public static final WrenchAction FACING = ofAnyDirection(Properties.FACING);
    public static final WrenchAction AXIS = of("axis", Properties.AXIS);
    public static final WrenchAction FACING_HORIZONTAL = of("facing", Properties.HORIZONTAL_FACING, (dir, next) -> next ? dir.rotateYClockwise() : dir.rotateYCounterclockwise());
    public static final WrenchAction HALF = of("half", Properties.BLOCK_HALF);
    public static final WrenchAction CHANNEL = ofChannel("channel", ChannelContainer.class,
        ChannelContainer::channel, ChannelContainer::setChannel
    );
    public static final WrenchAction FACING_HOPPER = WrenchAction.of("rotation", Properties.HOPPER_FACING);

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

    public static WrenchAction ofAnyDirection(DirectionProperty property) {
        return ofAnyDirection(property.getName(), property);
    }
    public static WrenchAction ofAnyDirection(String id, DirectionProperty property) {
        return of(id, property,
                (dir, next) -> FactoryUtil.REORDERED_DIRECTIONS.get(
                        (FactoryUtil.REORDERED_DIRECTIONS.size() + FactoryUtil.REORDERED_DIRECTIONS.indexOf(dir) + (next ? 1 : -1)) % FactoryUtil.REORDERED_DIRECTIONS.size()
                ));
    }

    public static <T> WrenchAction ofChannel(String id, Class<T> tClass, Function<T, Integer> get, BiConsumer<T, Integer> set) {
        return  ofBlockEntity(id,
                tClass,
                x -> String.valueOf(get.apply(x) + 1),
                (x, n) -> set.accept(x, (get.apply(x) + DataStorage.MAX_CHANNELS + (n ? 1 : -1)) % DataStorage.MAX_CHANNELS)
        );
    }
}
