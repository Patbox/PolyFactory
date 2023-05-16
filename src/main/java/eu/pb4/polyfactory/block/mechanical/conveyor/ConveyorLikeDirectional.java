package eu.pb4.polyfactory.block.mechanical.conveyor;

import net.minecraft.block.BlockState;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

import java.util.Locale;

public interface ConveyorLikeDirectional {
    TransferMode getTransferMode(BlockState selfState, Direction direction);

    enum TransferMode implements StringIdentifiable {
        BOTH(true, true),
        FROM_CONVEYOR(true, false),
        TO_CONVEYOR(false, true);

        public final boolean fromConveyor;
        public final boolean toConveyor;

        TransferMode(boolean fromConveyor, boolean toConveyor) {
            this.fromConveyor = fromConveyor;
            this.toConveyor = toConveyor;
        }

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
