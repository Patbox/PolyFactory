package eu.pb4.polyfactory.block.configurable;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.item.configuration.WrenchHandler;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import eu.pb4.polyfactory.ui.SimpleInputGui;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public record BlockConfig<T>(String id, Component name, Codec<T> codec, BlockConfigValue<T> value,
                             BlockValueFormatter<T> formatter,
                             WrenchModifyBlockValue<T> action, WrenchModifyBlockValue<T> alt) {
    public static final BlockConfig<Direction> FACING = ofDirection(BlockStateProperties.FACING);
    public static final BlockConfig<Direction.Axis> AXIS = of("axis", BlockStateProperties.AXIS);
    public static final BlockConfig<Direction.Axis> HORIZONTAL_AXIS = of("axis", BlockStateProperties.HORIZONTAL_AXIS);
    public static final BlockConfig<Direction> FACING_HORIZONTAL = ofDirection("facing", BlockStateProperties.HORIZONTAL_FACING);
    public static final BlockConfig<Half> HALF = of("half", BlockStateProperties.HALF, (t, world, pos, side, state) -> Component.translatable("text.polyfactory.half." + t.getSerializedName()));
    public static final BlockConfig<Integer> CHANNEL = ofChannel("channel", ChannelContainer.class,
            ChannelContainer::channel, ChannelContainer::setChannel
    );

    public static final BlockConfig<Integer> CHANNEL_WITH_DISABLED = ofChannelWithDisabled("channel", ChannelContainer.class,
            ChannelContainer::channel, ChannelContainer::setChannel
    );
    public static final BlockConfig<Direction> FACING_HOPPER = ofDirection("rotation", BlockStateProperties.FACING_HOPPER);
    public static final BlockConfig<Boolean> INVERTED = of("inverted", BlockStateProperties.INVERTED);
    public static final BlockConfig<Boolean> REVERSE = BlockConfig.of("reverse", FactoryProperties.REVERSE, (on, world, pos, side, state) -> CommonComponents.optionStatus(on));

    public static <T extends Comparable<T>> BlockConfig<T> of(Property<T> property) {
        return of(property.getName(), property);
    }    public static final BlockConfig<FrontAndTop> ORIENTATION = BlockConfig.of("orientation", BlockStateProperties.ORIENTATION, (dir, world, pos, side, state) ->
                    Component.empty().append(FactoryUtil.asText(dir.front())).append(" / ").append(FactoryUtil.asText(dir.top())),
            WrenchModifyBlockValue.ofProperty(BlockStateProperties.ORIENTATION),
            BlockConfigValue.ofProperty(BlockStateProperties.ORIENTATION)).withAlt(WrenchModifyBlockValue.ofAltOrientation(BlockStateProperties.ORIENTATION));

    public static <T extends Comparable<T>> BlockConfig<T> of(Property<T> property, BlockValueFormatter<T> textFunction) {
        return new BlockConfig<T>(property.getName(), Component.translatable("item.polyfactory.wrench.action." + property.getName()),
                Codec.stringResolver(property::getName, x -> property.getValue(x).orElse(property.getPossibleValues().getFirst())),
                BlockConfigValue.ofProperty(property),
                textFunction,
                WrenchModifyBlockValue.ofProperty(property), WrenchModifyBlockValue.ofProperty(property));
    }

    public static <T extends Comparable<T>> BlockConfig<T> of(String id, Property<T> property) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                FactoryUtil.propertyCodec(property),
                BlockConfigValue.ofProperty(property),
                BlockValueFormatter.getDefault(),
                WrenchModifyBlockValue.ofProperty(property), WrenchModifyBlockValue.ofProperty(property));
    }

    public static <T extends Comparable<T>> BlockConfig<T> of(String id, Property<T> property, BlockValueFormatter<T> textFunction) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                Codec.stringResolver(property::getName, x -> property.getValue(x).orElse(property.getPossibleValues().getFirst())),
                BlockConfigValue.ofProperty(property),
                textFunction,
                WrenchModifyBlockValue.ofProperty(property), WrenchModifyBlockValue.ofProperty(property));
    }

    public static <T extends Comparable<T>> BlockConfig<T> of(String id, Property<T> property, BiFunction<T, Boolean, T> function) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                FactoryUtil.propertyCodec(property),
                BlockConfigValue.ofProperty(property),
                BlockValueFormatter.getDefault(),
                WrenchModifyBlockValue.simple(function), WrenchModifyBlockValue.simple(function));
    }

    public static <T extends Comparable<T>> BlockConfig<T> of(String id, Property<T> property, BlockValueFormatter<T> textFunction, BiFunction<T, Boolean, T> function) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                FactoryUtil.propertyCodec(property),
                BlockConfigValue.ofProperty(property),
                textFunction,
                WrenchModifyBlockValue.simple(function), WrenchModifyBlockValue.simple(function));
    }

    public static <T extends Comparable<T>> BlockConfig<T> of(String id, Property<T> property, BlockValueFormatter<T> textFunction, WrenchModifyBlockValue<T> function) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                FactoryUtil.propertyCodec(property),
                BlockConfigValue.ofProperty(property),
                textFunction,
                function, function);
    }

    public static <T extends Comparable<T>> BlockConfig<T> of(String id, Property<T> property, BlockValueFormatter<T> textFunction, BiFunction<T, Boolean, T> function, BlockConfigValue<T> value) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                FactoryUtil.propertyCodec(property),
                value,
                textFunction,
                WrenchModifyBlockValue.simple(function), WrenchModifyBlockValue.simple(function));
    }

    public static <T extends Comparable<T>> BlockConfig<T> of(String id, Property<T> property, BlockValueFormatter<T> textFunction, WrenchModifyBlockValue<T> function, BlockConfigValue<T> value) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                FactoryUtil.propertyCodec(property),
                value,
                textFunction,
                function, function);
    }

    public static <T> BlockConfig<T> of(String id, Codec<T> codec, BlockConfigValue<T> value, WrenchModifyBlockValue<T> action) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id), codec,
                value,
                BlockValueFormatter.getDefault(), action, action);
    }

    public static <T> BlockConfig<T> of(String id, Codec<T> codec, BlockConfigValue<T> value, BlockValueFormatter<T> formatter, WrenchModifyBlockValue<T> action) {
        return new BlockConfig<T>(id, Component.translatable("item.polyfactory.wrench.action." + id), codec,
                value,
                formatter, action, action);
    }

    public static BlockConfig<Direction> ofDirection(EnumProperty<Direction> property) {
        return ofDirection(property.getName(), property);
    }

    public static BlockConfig<Direction> ofDirection(String id, EnumProperty<Direction> property) {

        return of(id, property, (dir, world, pos, side, state) -> FactoryUtil.asText(dir),
                WrenchModifyBlockValue.ofDirection(property)
        ).withAlt(WrenchModifyBlockValue.ofAltDirection(property));
    }

    public static <T, BE> BlockConfig<T> ofBlockEntity(String id, Codec<T> codec, Class<BE> beClass,
                                                       BlockValueFormatter<T> formatter, Function<BE, T> getter, BiConsumer<BE, T> setter,
                                                       WrenchModifyBlockValue<T> action) {
        return of(id, codec, BlockConfigValue.ofBlockEntity(beClass, getter, setter), formatter, action);
    }

    public static <BE> BlockConfig<Integer> ofChannel(String id, Class<BE> tClass, Function<BE, Integer> get, BiConsumer<BE, Integer> set) {
        return ofBlockEntityInt(id, tClass, 0, DataStorage.MAX_CHANNELS - 1, 1, get, set);
    }

    public static <BE> BlockConfig<Integer> ofChannelWithDisabled(String id, Class<BE> tClass, Function<BE, Integer> get, BiConsumer<BE, Integer> set) {
        return ofBlockEntityInt(id, tClass, -1, DataStorage.MAX_CHANNELS - 1, 1,
                x -> x != 0 ? String.valueOf(x) : "X", get, set);
    }

    public static <BE> BlockConfig<Integer> ofBlockEntityInt(String id, Class<BE> tClass, int minInclusive, int maxInclusive, int displayOffset, Function<BE, Integer> get, BiConsumer<BE, Integer> set) {
        return ofBlockEntityInt(id, tClass, minInclusive, maxInclusive, displayOffset, String::valueOf, get, set);

    }

    public static <BE> BlockConfig<Integer> ofBlockEntityInt(String id, Class<BE> tClass, int minInclusive, int maxInclusive, IntFunction<String> display, Function<BE, Integer> get, BiConsumer<BE, Integer> set) {
        return ofBlockEntityInt(id, tClass, minInclusive, maxInclusive, 0, display, get, set);
    }

    public static <BE> BlockConfig<Integer> ofBlockEntityInt(String id, Class<BE> tClass, int minInclusive, int maxInclusive, int displayOffset, IntFunction<String> display, Function<BE, Integer> get, BiConsumer<BE, Integer> set) {
        var tmp = ofBlockEntity(id,
                Codec.INT,
                tClass,
                (x, world, pos, side, state) -> Component.literal(display.apply(x + displayOffset)),
                get,
                set,
                WrenchModifyBlockValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 1 : -1), minInclusive, maxInclusive)));
        var name = tmp.name;
        return tmp.withAlt((value, next, player, world, pos, side, state) -> {
            var be = world.getBlockEntity(pos);
            if (player instanceof ServerPlayer serverPlayer && tClass.isInstance(be)) {
                new SimpleInputGui(serverPlayer,
                        Component.translatable("item.polyfactory.wrench.ui.set_to_range", name, minInclusive + displayOffset, maxInclusive + displayOffset),
                        be::isRemoved,
                        String.valueOf(value + displayOffset),
                        x -> {
                            try {
                                var val = Integer.parseInt(x) - displayOffset;
                                return val >= minInclusive && val <= maxInclusive;
                            } catch (Throwable e) {
                                // ignore
                            }

                            return false;
                        }, x -> {
                    try {
                        //noinspection unchecked
                        set.accept((BE) be, Integer.parseInt(x) - displayOffset);
                        WrenchHandler.of(serverPlayer).forceUpdate();
                    } catch (Throwable e) {
                        // ignore
                    }
                });
                serverPlayer.swing(InteractionHand.MAIN_HAND, true);
            }
            return value;
        });
    }

    public BlockConfig<T> withAlt(WrenchModifyBlockValue<T> alt) {
        return new BlockConfig<T>(this.id, this.name, this.codec, this.value, this.formatter, this.action, alt);
    }

    public Component getDisplayValue(Level world, BlockPos blockPos, Direction side, BlockState state) {
        return this.formatter.getDisplayValue(this.value.getValue(world, blockPos, side, state), world, blockPos, side, state);
    }

    public BlockConfig<T> withValue(BlockConfigValue<T> valueSetter) {
        return new BlockConfig<>(this.id, this.name, this.codec, valueSetter, this.formatter, this.action, alt);
    }
}
