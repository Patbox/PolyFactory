package eu.pb4.polyfactory.block.property;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

import java.util.Locale;

public enum ConnectablePart implements StringIdentifiable {
    NEGATIVE(Direction.AxisDirection.NEGATIVE),
    MIDDLE(null),
    POSITIVE(Direction.AxisDirection.POSITIVE),
    SINGLE(null);

    private Direction.AxisDirection axisDirection;
    ConnectablePart(Direction.AxisDirection axisDirection) {
        this.axisDirection = axisDirection;
    }

    public Direction.AxisDirection axisDirection() {
        return this.axisDirection;
    }

    public boolean middle() {
        return this == MIDDLE;
    }

    public boolean single() {
        return this == SINGLE;
    }

    public boolean positive() {
        return this == POSITIVE;
    }

    public boolean negative() {
        return this == NEGATIVE;
    }

    @Override
    public String asString() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public ConnectablePart negate() {
        return this == POSITIVE ? NEGATIVE : this == NEGATIVE ? POSITIVE : this;
    }
}