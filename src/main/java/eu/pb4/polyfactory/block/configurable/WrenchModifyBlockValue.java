package eu.pb4.polyfactory.block.configurable;

import eu.pb4.polyfactory.item.configuration.WrenchHandler;
import eu.pb4.polyfactory.ui.SimpleInputGui;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

public interface WrenchModifyBlockValue<T> {
    @SuppressWarnings({"unchecked", "rawtypes", "RedundantCast"})
    static <T extends Comparable<T>> WrenchModifyBlockValue<T> ofProperty(Property<T> property) {
        return (value, next, player, world, pos, side, state) -> {
            var elements = property.getPossibleValues();
            return !next ? Util.findPreviousInIterable(elements, value) : Util.findNextInIterable(elements, value);
        };
    }

    static <T> WrenchModifyBlockValue<T> simple(BiFunction<T, Boolean, T> transformer) {
        return (value, next, player, world, pos, side, state) -> transformer.apply(value, next);
    }

    static <T> WrenchModifyBlockValue<T> simple(List<T> values) {
        return (value, next, player, world, pos, side, state) -> next ? Util.findNextInIterable(values, value) : Util.findPreviousInIterable(values, value);
    }

    static <T> WrenchModifyBlockValue<T> simple(T... values) {
        return simple(List.of(values));
    }

    @SafeVarargs
    static <T extends Enum<T>> WrenchModifyBlockValue<T> enums(T... values) {
        return (value, next, player, world, pos, side, state) -> FactoryUtil.nextEnum(value, values, next);
    }

    static WrenchModifyBlockValue<Direction> ofDirection(EnumProperty<Direction> property) {
        var reordered = new ArrayList<Direction>();
        for (var x : FactoryUtil.REORDERED_DIRECTIONS) {
            if (property.getPossibleValues().contains(x)) {
                reordered.add(x);
            }
        }
        return (dir, next, player, world, pos, side, state) -> reordered.get((reordered.size() + reordered.indexOf(dir) + (next ? 1 : -1)) % reordered.size());
    }

    static WrenchModifyBlockValue<Direction> ofAltDirection(EnumProperty<Direction> property) {
        return (dir, next, player, world, pos, side, state) -> {
            var val = next ? side : side.getOpposite();
            return property.getPossibleValues().contains(val) ? val : dir;
        };
    }

    static WrenchModifyBlockValue<FrontAndTop> ofAltOrientation(EnumProperty<FrontAndTop> property) {
        return (dir, next, player, world, pos, side, state) -> {
            var val = next ? side : side.getOpposite();
            var dir2 = switch (val) {
                case DOWN -> player.getDirection();
                case UP -> player.getDirection().getOpposite();
                default -> Direction.UP;
            };

            var orientation = FrontAndTop.fromFrontAndTop(val, dir2);
            return property.getPossibleValues().contains(orientation) ? orientation : dir;
        };
    }


    static WrenchModifyBlockValue<Integer> ofAltIntegerInput(Component name, int minInclusive, int maxInclusive, int displayOffset,
                                                             TriConsumer<Level, BlockPos, Integer> consumer, BiPredicate<Level, BlockPos> predicate, IntFunction<String> display) {
        return (value, next, player, world, pos, side, state) -> {
            if (player instanceof ServerPlayer serverPlayer && predicate.test(world, pos)) {
                new SimpleInputGui(serverPlayer,
                        Component.translatable("item.polyfactory.wrench.ui.set_to_range", name, display.apply(minInclusive + displayOffset), display.apply(maxInclusive + displayOffset)),
                        () -> !predicate.test(world, pos),
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
                        consumer.accept(world, pos,Integer.parseInt(x) - displayOffset);
                        WrenchHandler.of(serverPlayer).forceUpdate();
                    } catch (Throwable e) {
                        // ignore
                    }
                });
                serverPlayer.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            }
            return value;
        };
    }

    static WrenchModifyBlockValue<Float> ofAltFloatInput(Component name, float minInclusive, float maxInclusive, float displayOffset, float rawInputScale,
                                                           TriConsumer<Level, BlockPos, Float> consumer, BiPredicate<Level, BlockPos> predicate,
                                                           DoubleFunction<String> display) {
        return (value, next, player, world, pos, side, state) -> {
            if (player instanceof ServerPlayer serverPlayer && predicate.test(world, pos)) {
                new SimpleInputGui(serverPlayer,
                        Component.translatable("item.polyfactory.wrench.ui.set_to_range", name, display.apply(minInclusive + displayOffset), display.apply(maxInclusive + displayOffset)),
                        () -> !predicate.test(world, pos),
                        String.valueOf(value / rawInputScale + displayOffset),
                        x -> {
                            try {
                                var val = Float.parseFloat(x) * rawInputScale - displayOffset;
                                return val >= minInclusive && val <= maxInclusive;
                            } catch (Throwable e) {
                                // ignore
                            }

                            return false;
                        }, x -> {
                    try {
                        //noinspection unchecked
                        consumer.accept(world, pos,Float.parseFloat(x) * rawInputScale - displayOffset);
                        WrenchHandler.of(serverPlayer).forceUpdate();
                    } catch (Throwable e) {
                        // ignore
                    }
                });
                serverPlayer.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            }
            return value;
        };
    }


    static <T, BE> WrenchModifyBlockValue<T> ofAltCustomInputBlockEntity(Component name,
                                                                         Class<BE> tClass, BiConsumer<BE, T> setter,
                                                          ThrowingFunction<String, T> parser,
                                                          Function<T, String> stringifier) {
        return ofAltCustomInput(name, (level, pos, value) -> {
            var be = level.getBlockEntity(pos);
            if (tClass.isInstance(be)) {
                setter.accept((BE) be, value);
            }
        }, parser, stringifier, (level, pos) -> {
            var be = level.getBlockEntity(pos);
            return tClass.isInstance(be) && !be.isRemoved();
        });
    }

    static <T> WrenchModifyBlockValue<T> ofAltCustomInput(Component name,
                                                          TriConsumer<Level, BlockPos, T> consumer,
                                                          ThrowingFunction<String, T> parser,
                                                          Function<T, String> stringifier,
                                                          BiPredicate<Level, BlockPos> validTargetPredicate) {
        return (value, next, player, world, pos, side, state) -> {
            if (player instanceof ServerPlayer serverPlayer && validTargetPredicate.test(world, pos)) {
                new SimpleInputGui(serverPlayer,
                        name,
                        () -> !validTargetPredicate.test(world, pos),
                        stringifier.apply(value),
                        x -> {
                            try {
                                return parser.apply(x) != null;
                            } catch (Throwable e) {
                                // ignore
                            }

                            return false;
                        }, x -> {
                    try {
                        //noinspection unchecked
                        consumer.accept(world, pos,parser.apply(x));
                        WrenchHandler.of(serverPlayer).forceUpdate();
                    } catch (Throwable e) {
                        // ignore
                    }
                });
                serverPlayer.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            }
            return value;
        };
    }

    T modifyValue(T value, boolean next, Player player, Level world, BlockPos pos, Direction side, BlockState state);


    interface ThrowingFunction<A, B> {
        B apply(A value) throws Throwable;
    }
}
