package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.StringIdentifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record DataType<T extends DataContainer>(String id, MapCodec<T> codec) implements StringIdentifiable {
    private static final Map<String, DataType<DataContainer>> TYPES = new HashMap<>();

    @SuppressWarnings("unchecked")
    public DataType {
        TYPES.put(id, (DataType<DataContainer>) this);
    }

    public static final DataType<BoolData> BOOL = new DataType<>("bool", BoolData.TYPE_CODEC);
    public static final DataType<LongData> LONG = new DataType<>("long", LongData.TYPE_CODEC);
    public static final DataType<DoubleData> DOUBLE = new DataType<>("double", DoubleData.TYPE_CODEC);
    public static final DataType<StringData> STRING = new DataType<>("string", StringData.TYPE_CODEC);
    public static final DataType<GameEventData> GAME_EVENT = new DataType<>("game_event", GameEventData.TYPE_CODEC);
    public static final DataType<BlockStateData> BLOCK_STATE = new DataType<>("block_state", BlockStateData.TYPE_CODEC);
    public static final DataType<ItemStackData> ITEM_STACK = new DataType<>("item_stack", ItemStackData.TYPE_CODEC);
    public static final DataType<CapacityData> CAPACITY = new DataType<>("capacity", CapacityData.TYPE_CODEC);
    public static final DataType<InvalidData> INVALID = new DataType<>("invalid", InvalidData.TYPE_CODEC);

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final Codec<DataType<?>> CODEC = Codec.STRING.xmap(x -> TYPES.getOrDefault(x, (DataType) STRING), DataType::id);

    @Override
    public String asString() {
        return this.id;
    }
}
