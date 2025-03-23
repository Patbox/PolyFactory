package eu.pb4.polyfactory.util;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;

public enum OptionalDirection implements StringIdentifiable {
    NORTH(Direction.NORTH),
    EAST(Direction.EAST),
    SOUTH(Direction.SOUTH),
    WEST(Direction.WEST),
    UP(Direction.UP),
    DOWN(Direction.DOWN),
    NONE(null);

    public static final Codec<OptionalDirection> CODEC = StringIdentifiable.createCodec(OptionalDirection::values);

    private final String name;
    @Nullable
    private final Direction direction;
    OptionalDirection(@Nullable Direction direction) {
        this.name = direction != null ? direction.asString() : "none";
        this.direction = direction;
    }

    public static OptionalDirection of(@Nullable Direction facing) {
        return switch (facing) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case null -> NONE;
        };
    }

    @Override
    public String asString() {
        return this.name;
    }

    @Nullable
    public Direction direction() {
        return this.direction;
    }

    public Vec3i getVector() {
        return this.direction == null ? Vec3i.ZERO : this.direction.getVector();
    }

    public Vec3d getDoubleVector() {
        return this.direction == null ? Vec3d.ZERO : this.direction.getDoubleVector();
    }

    public Text asText() {
        return FactoryUtil.asText(this.direction);
    }
}
