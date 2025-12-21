package eu.pb4.polyfactory.block.configurable;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public interface BlockConfigValue<T> {
    static <T, B> BlockConfigValue<T> ofBlockEntity(Class<B> beClass, Function<B, T> getter, BiConsumer<B, T> setter) {
        return new BlockConfigValue<T>() {
            @Nullable
            @Override
            public T getValue(Level world, BlockPos pos, Direction side, BlockState state) {
                var be = world.getBlockEntity(pos);

                if (be != null && beClass.isAssignableFrom(be.getClass())) {
                    //noinspection unchecked
                    return getter.apply((B) be);
                }
                return null;
            }

            @Override
            public boolean setValue(T value, Level world, BlockPos pos, Direction side, BlockState state) {
                var be = world.getBlockEntity(pos);

                if (be != null && beClass.isAssignableFrom(be.getClass())) {
                    //noinspection unchecked
                    setter.accept((B) be, value);
                    return true;
                }
                return false;
            }
        };
    }

    static <T extends Comparable<T>> BlockConfigValue<T> ofProperty(Property<T> property) {
        return ofPropertyCustom(property, (StateProvider<T>) (propertyx, value, world, pos, side, state) -> state.trySetValue(propertyx, value));
    }

    static <T extends Comparable<T>> BlockConfigValue<T> ofPropertyCustom(Property<T> property, StateProvider<T> provider) {
        return new BlockConfigValue<T>() {
            @Nullable
            @Override
            public T getValue(Level world, BlockPos pos, Direction side, BlockState state) {
                return state.getValueOrElse(property, null);
            }

            @Override
            public boolean setValue(T value, Level world, BlockPos pos, Direction side, BlockState state) {
                var newState = provider.getModifiedState(property, value, world, pos, side, state);

                if (newState == state) {
                    return false;
                }

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
                        world.setBlockAndUpdate(pos, newState);
                        for (var el : holder.holder().getElements()) {
                            if (el instanceof DisplayElement displayElement) {
                                displayElement.setTeleportDuration(map.getInt(displayElement));
                                displayElement.setInterpolationDuration(map2.getInt(displayElement));
                                displayElement.tick();
                            }
                        }
                        return true;
                    }
                }
                world.setBlockAndUpdate(pos, newState);
                return true;
            }
        };
    }

    @Nullable
    T getValue(Level world, BlockPos pos, Direction side, BlockState state);
    boolean setValue(T value, Level world, BlockPos pos, Direction side, BlockState state);


    interface StateProvider<T extends  Comparable<T>> {
        BlockState getModifiedState(Property<T> property, T value, Level world, BlockPos pos, Direction side, BlockState state);
    }
}
