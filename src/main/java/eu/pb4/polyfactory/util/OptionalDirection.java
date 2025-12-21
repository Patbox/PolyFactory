package eu.pb4.polyfactory.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public enum OptionalDirection implements StringRepresentable {
    NORTH(Direction.NORTH),
    EAST(Direction.EAST),
    SOUTH(Direction.SOUTH),
    WEST(Direction.WEST),
    UP(Direction.UP),
    DOWN(Direction.DOWN),
    NONE(null);

    public static final Codec<OptionalDirection> CODEC = StringRepresentable.fromEnum(OptionalDirection::values);

    private final String name;
    @Nullable
    private final Direction direction;
    OptionalDirection(@Nullable Direction direction) {
        this.name = direction != null ? direction.getSerializedName() : "none";
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
    public String getSerializedName() {
        return this.name;
    }

    @Nullable
    public Direction direction() {
        return this.direction;
    }

    public Vec3i getVector() {
        return this.direction == null ? Vec3i.ZERO : this.direction.getUnitVec3i();
    }

    public Vec3 getDoubleVector() {
        return this.direction == null ? Vec3.ZERO : this.direction.getUnitVec3();
    }

    public Component asText() {
        return FactoryUtil.asText(this.direction);
    }
}
