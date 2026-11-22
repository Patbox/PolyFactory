package eu.pb4.polyfactory.block.configurable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockValueFormatter<T> {
    static <T> BlockValueFormatter<T> getDefault() {
        return (val, world, pos, side, state) -> Component.literal(String.valueOf(val));
    }

    static <T extends StringRepresentable> BlockValueFormatter<T> subTranslations(String key) {
        return (val, world, pos, side, state) -> Component.translatable( key + "." + val.getSerializedName());
    }

    static <T extends StringRepresentable> BlockValueFormatter<T> actionSubTranslations(String id) {
        return subTranslations("item.polyfactory.wrench.action." + id);
    }

    Component getDisplayValue(T value, Level world, BlockPos pos, Direction side, BlockState state);


    static <T> BlockValueFormatter<T> text(TextFunc<T> format) {
        return ((value, world, pos, side, state) -> format.apply(value));
    }

    static <T> BlockValueFormatter<T> str(StringFunc<T> format) {
        return ((value, world, pos, side, state) -> Component.literal(format.apply(value)));
    }

    interface TextFunc<T> {
        Component apply(T value);
    }

    interface StringFunc<T> {
        String apply(T value);
    }
}
