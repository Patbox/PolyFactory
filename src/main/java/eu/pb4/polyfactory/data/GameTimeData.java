package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;

public record GameTimeData(long day, long dayTime, long gameTime) implements DataContainer {
    public static MapCodec<GameTimeData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.fieldOf("day").forGetter(GameTimeData::day),
            Codec.LONG.fieldOf("day_time").forGetter(GameTimeData::dayTime),
            Codec.LONG.fieldOf("game_time").forGetter(GameTimeData::gameTime)
    ).apply(instance, GameTimeData::new));


    public static GameTimeData now(ServerLevel level) {
        return new GameTimeData(level.getDayCount(), level.getDayTime() % 24000, level.getGameTime());
    }

    @Override
    public DataType<GameTimeData> type() {
        return DataType.GAME_TIME;
    }

    @Override
    public String asString() {
        long dayTime = this.dayTimeAsVirtualMinutes();
        return String.format("%sd %02d:%02d", this.day, (dayTime / 60  + 6) % 24, dayTime % 60);
    }

    public String asTimeString() {
        return asTimeString(this.dayTime);
    }

    public static String asTimeString(long dayTime) {
        dayTime = dayTimeAsVirtualMinutes(dayTime);
        return String.format("%02d:%02d", (dayTime / 60 + 6) % 24, dayTime % 60);
    }

    public long dayTimeAsVirtualMinutes() {
        return dayTimeAsVirtualMinutes(this.dayTime);
    }

    public static long dayTimeAsVirtualMinutes(long dayTime) {
        return (long) (dayTime * 3.6 / 60);
    }

    @Override
    public long asLong() {
        return this.gameTime;
    }

    @Override
    public double asDouble() {
        return asLong();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public DataContainer extract(String field) {
        return switch (field) {
            case "game_time" -> new LongData(this.gameTime);
            case "day_time" -> new LongData(this.dayTime);
            case "day_time_text" -> new StringData(this.asTimeString());
            case "day_hour" -> new LongData(this.dayTimeAsVirtualMinutes() / 60);
            case "day_minute" -> new LongData(this.dayTimeAsVirtualMinutes() % 60);
            case "day_minutes" -> new LongData(this.dayTimeAsVirtualMinutes());
            case "day" -> new LongData(this.day);
            default -> DataContainer.super.extract(field);
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        GameTimeData that = (GameTimeData) object;
        return this.gameTime == that.gameTime;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.gameTime);
    }

    @Override
    public int compareTo(DataContainer other) {
        return Long.compare(this.gameTime, other.asLong());
    }
}
