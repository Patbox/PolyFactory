package eu.pb4.polyfactory.block.other;

import com.mojang.serialization.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.BlockState;

public interface StatePropertiesCodecPatcher {
    static MapCodec<BlockState> modifier(MapCodec<BlockState> codec, Modifier modifier) {
        return new MapCodec<>() {
            @Override
            public <T> RecordBuilder<T> encode(BlockState input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return codec.encode(input, ops, prefix);
            }

            @Override
            public <T> DataResult<BlockState> decode(DynamicOps<T> ops, MapLike<T> input) {
                var result = codec.decode(ops, input);
                if (result.result().isPresent()) {
                    var state = result.result().get();
                    //noinspection unchecked
                    return DataResult.success(modifier.modify(state, (DynamicOps<Object>) ops, (MapLike<Object>) input));
                }

                return result;
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return codec.keys(ops);
            }
        };
    }

    MapCodec<BlockState> modifyPropertiesCodec(MapCodec<BlockState> codec);

    interface Modifier {
        BlockState modify(BlockState state, DynamicOps<Object> ops, MapLike<Object> input);
    }
}
