package eu.pb4.polyfactory.item.wrench;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface WrenchApplyAction {
    static <T> WrenchApplyAction ofBlockEntity(Class<T> beClass, BiConsumer<T, Boolean> value) {
        return (player, world, pos, side, state, next) -> {
            var be = world.getBlockEntity(pos);

            if (be != null && beClass.isAssignableFrom(be.getClass())) {
                value.accept((T) be, next);
                return true;
            }

            return false;
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes", "RedundantCast"})
    static WrenchApplyAction ofProperty(Property<?> property) {
        return ofProperty((Property) (Object) property, (Comparable current, Boolean next) -> {
            var elements = (Collection<Comparable>) property.getValues();
            return !next ? Util.previous(elements, current) : Util.next(elements, current);
        });
    }

    static <T extends Comparable<T>> WrenchApplyAction ofProperty(Property<T> property, BiFunction<T, Boolean, T> function) {
        return ofState((state, next) -> state.contains(property) ? state.with(property, function.apply(state.get(property), next)) : state);
    }

    static <T extends Comparable<T>> WrenchApplyAction ofState(BiFunction<BlockState, Boolean, BlockState> function) {
        return ofState(StateAction.of(function));
    }
    static <T extends Comparable<T>> WrenchApplyAction ofState(StateAction function) {
        return (player, world, pos, side, state, next) -> {
            if (state.getBlock() instanceof FactoryBlock) {
                var holder = BlockAwareAttachment.get(world, pos);
                if (holder != null) {
                    var map = new Object2IntArrayMap<>();
                    var map2 = new Object2IntArrayMap<>();
                    for (var el : holder.holder().getElements()) {
                        if (el instanceof DisplayElement displayElement) {
                            map.put(displayElement, displayElement.getTeleportDuration());
                            map2.put(displayElement, displayElement.getInterpolationDuration());
                            displayElement.setTeleportDuration(0);
                            displayElement.setInterpolationDuration(0);
                            displayElement.tick();
                        }
                    }
                    var x = world.setBlockState(pos, function.apply(player, world, pos, side, state, next));
                    for (var el : holder.holder().getElements()) {
                        if (el instanceof DisplayElement displayElement) {
                            displayElement.setTeleportDuration(map.getInt(displayElement));
                            displayElement.setInterpolationDuration(map2.getInt(displayElement));
                            displayElement.tick();
                        }
                    }
                    return x;
                }
            }

            return world.setBlockState(pos, function.apply(player, world, pos, side, state, next));
        };
    }

    boolean applyAction(PlayerEntity player, World world, BlockPos pos, Direction side, BlockState state, boolean next);

    interface StateAction {
        BlockState apply(PlayerEntity entity, World world, BlockPos pos, Direction side, BlockState state, boolean next);
        static StateAction of(BiFunction<BlockState, Boolean, BlockState> action) {
            return ((player, world, pos, dir, state, next) -> action.apply(state, next));
        }
    }
}
