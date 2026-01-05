package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public record DataType<T extends DataContainer>(String id, MapCodec<T> codec, List<String> fields) implements StringRepresentable {
    private static final Map<String, DataType<DataContainer>> TYPES = new HashMap<>();

    public DataType(String id, MapCodec<T> codec, String... fields) {
        this(id, codec, List.of(fields));
    }

    @SuppressWarnings("unchecked")
    public DataType {
        TYPES.put(id, (DataType<DataContainer>) this);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final Codec<DataType<?>> CODEC = Codec.lazyInitialized(() -> Codec.STRING.xmap(x -> DataType.TYPES.getOrDefault(x, (DataType) DataType.STRING), DataType::id));

    public static final DataType<BoolData> BOOL = new DataType<>("bool", BoolData.TYPE_CODEC);
    public static final DataType<LongData> LONG = new DataType<>("long", LongData.TYPE_CODEC);
    public static final DataType<DoubleData> DOUBLE = new DataType<>("double", DoubleData.TYPE_CODEC);
    public static final DataType<StringData> STRING = new DataType<>("string", StringData.TYPE_CODEC, "length", "charat:*", "substring:*");

    public static final DataType<RedstoneData> REDSTONE = new DataType<>("redstone", RedstoneData.TYPE_CODEC);
    public static final DataType<ProgressData> PROGRESS = new DataType<>("progress", ProgressData.TYPE_CODEC);
    public static final DataType<GameEventData> GAME_EVENT = new DataType<>("game_event", GameEventData.TYPE_CODEC, "event", "position", "distance");
    public static final DataType<SoundEventData> SOUND_EVENT = new DataType<>("sound_event", SoundEventData.TYPE_CODEC, "sound", "volume", "pitch");
    public static final DataType<BlockStateData> BLOCK_STATE = new DataType<>("block_state", BlockStateData.TYPE_CODEC, "type", "property:*");
    public static final DataType<ItemStackData> ITEM_STACK = new DataType<>("item_stack", ItemStackData.TYPE_CODEC, "type", "count", "name", "damage");
    public static final DataType<CapacityData> CAPACITY = new DataType<>("capacity", CapacityData.TYPE_CODEC, "stored", "capacity", "percent");
    public static final DataType<GameTimeData> GAME_TIME = new DataType<>("game_time", GameTimeData.TYPE_CODEC, "day", "day_time", "game_time", "day_hour", "day_minute", "day_minutes");
    public static final DataType<InvalidData> INVALID = new DataType<>("invalid", InvalidData.TYPE_CODEC);
    public static final DataType<MapData> MAP = new DataType<>("list", MapData.TYPE_CODEC, "size", "key:*", "exists:*");
    public static final DataType<ListData> LIST = new DataType<>("list", ListData.TYPE_CODEC, "size", "key:*");
    public static final DataType<EmptyData> EMPTY = new DataType<>("empty", EmptyData.TYPE_CODEC);

    public static Collection<DataType<DataContainer>> types() {
        return TYPES.values();
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public Component name() {
        return Component.translatable("data_type.polyfactory." + this.id);
    }
}
