package eu.pb4.polyfactory.item.wrench;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.Function;

public interface WrenchValueGetter {
    static <T> WrenchValueGetter ofBlockEntity(Class<T> beClass, Function<T, String> value) {
        return (world, pos, side, state) -> {
            var be = world.getBlockEntity(pos);

            if (be != null && beClass.isAssignableFrom(be.getClass())) {
                return value.apply((T) be);
            }

            return "<*>";
        };
    }

    String getDisplayValue(World world, BlockPos pos, Direction side, BlockState state);


    @SuppressWarnings({"unchecked", "rawtypes"})
    static WrenchValueGetter ofProperty(Property<?> property) {
        return (world, pos, side, state) -> state.getOrEmpty(property).map((x) -> ((Property) property).name(x)).orElse("<*>");
    }
}
