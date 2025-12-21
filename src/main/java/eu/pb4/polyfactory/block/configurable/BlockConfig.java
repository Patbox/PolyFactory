package eu.pb4.polyfactory.block.configurable;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import eu.pb4.polyfactory.util.FactoryUtil;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;

public record BlockConfig<T>(String id, Component name, Codec<T> codec, BlockConfigValue<T> value, BlockValueFormatter<T> formatter,
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
    }

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
        return ofBlockEntityInt(id, tClass, -1, DataStorage.MAX_CHANNELS - 1,
                x -> x != -1 ? String.valueOf(x + 1) : "X", get, set);
    }

    public static <BE> BlockConfig<Integer> ofBlockEntityInt(String id, Class<BE> tClass, int minInclusive, int maxInclusive, int displayOffset, Function<BE, Integer> get, BiConsumer<BE, Integer> set) {
        return ofBlockEntity(id,
                Codec.INT,
                tClass,
                (x, world, pos, side, state) -> Component.literal(String.valueOf(x + displayOffset)),
                get,
                set,
                WrenchModifyBlockValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 1 : -1), minInclusive, maxInclusive)));
    }

    public static <BE> BlockConfig<Integer> ofBlockEntityInt(String id, Class<BE> tClass, int minInclusive, int maxInclusive, IntFunction<String> display, Function<BE, Integer> get, BiConsumer<BE, Integer> set) {
        return ofBlockEntity(id,
                Codec.INT,
                tClass,
                (x, world, pos, side, state) -> Component.literal(display.apply(x)),
                get,
                set,
                WrenchModifyBlockValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 1 : -1), minInclusive, maxInclusive)));
    }

    public BlockConfig<T> withAlt(WrenchModifyBlockValue<T> alt) {
        return new BlockConfig<T>(this.id, this.name, this.codec, this.value, this.formatter, this.action, alt);
    }

    public Component getDisplayValue(Level world, BlockPos blockPos, Direction side, BlockState state) {
        return this.formatter.getDisplayValue(this.value.getValue(world, blockPos, side, state), world, blockPos, side, state);
    }
}
