package eu.pb4.polyfactory.block.configurable;

import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface BlockValueFormatter<T> {
    static <T> BlockValueFormatter<T> getDefault() {
        return (val, world, pos, side, state) -> Text.literal(String.valueOf(val));
    }

    Text getDisplayValue(T value, World world, BlockPos pos, Direction side, BlockState state);


    static <T> BlockValueFormatter<T> text(TextFunc<T> format) {
        return ((value, world, pos, side, state) -> format.apply(value));
    }

    static <T> BlockValueFormatter<T> str(StringFunc<T> format) {
        return ((value, world, pos, side, state) -> Text.literal(format.apply(value)));
    }

    interface TextFunc<T> {
        Text apply(T value);
    }

    interface StringFunc<T> {
        String apply(T value);
    }
}
