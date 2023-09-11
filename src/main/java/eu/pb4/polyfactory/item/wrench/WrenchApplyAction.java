package eu.pb4.polyfactory.item.wrench;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface WrenchApplyAction {
    void applyAction(World world, BlockPos pos, Direction side, BlockState state, boolean next);

    static <T> WrenchApplyAction ofBlockEntity(Class<T> beClass, BiConsumer<T, Boolean> value) {
        return (world, pos, side, state, next) -> {
            var be = world.getBlockEntity(pos);

            if (be != null && beClass.isAssignableFrom(be.getClass())) {
                value.accept((T) be, next);
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static WrenchApplyAction ofProperty(Property<?> property) {
        return (world, pos, side, state, next) -> {
            if (state.contains(property)) {
                var elements = (Collection) property.getValues();
                var current = (Object) state.get(property);
                world.setBlockState(pos, state.with((Property) property, (Comparable) (!next ? Util.previous(elements, current) : Util.next(elements, current))));
            }
        };
    }
}
