package eu.pb4.polyfactory.block.property;

import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum ConnectablePart implements StringRepresentable {
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

    public boolean hasNext() {
        return negative() || middle();
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public ConnectablePart negate() {
        return this == POSITIVE ? NEGATIVE : this == NEGATIVE ? POSITIVE : this;
    }
}