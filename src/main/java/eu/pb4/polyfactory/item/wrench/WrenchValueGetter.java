package eu.pb4.polyfactory.item.wrench;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.function.Function;

public interface WrenchValueGetter {
    static <T> WrenchValueGetter ofBlockEntity(Class<T> beClass, Function<T, Text> value) {
        return (world, pos, side, state) -> {
            var be = world.getBlockEntity(pos);

            if (be != null && beClass.isAssignableFrom(be.getClass())) {
                return value.apply((T) be);
            }

            return Text.literal("<*>");
        };
    }

    Text getDisplayValue(World world, BlockPos pos, Direction side, BlockState state);

    static <T extends Comparable<T>> WrenchValueGetter ofProperty(Property<T> property, Function<T, Text> textFunction) {
        return (world, pos, side, state) -> state.getOrEmpty(property).map(textFunction).orElse(Text.empty());
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    static WrenchValueGetter ofProperty(Property<?> property) {
        return (world, pos, side, state) -> Text.literal(state.getOrEmpty(property).map((x) -> ((Property) property).name(x)).orElse("<*>"));
    }
}
