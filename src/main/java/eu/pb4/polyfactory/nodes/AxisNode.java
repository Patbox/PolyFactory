package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.HalfLink;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

public interface AxisNode extends DirectionCheckingNode {
    Direction.Axis axis();

    @Override
    default boolean canConnectDir(Direction direction) {
        return this.axis() == direction.getAxis();
    }
}
