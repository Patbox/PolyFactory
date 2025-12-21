package eu.pb4.polyfactory.block.mechanical.conveyor;

import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.BlockState;

public interface ConveyorLikeDirectional {
    TransferMode getTransferMode(BlockState selfState, Direction direction);

    enum TransferMode implements StringRepresentable {
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
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
