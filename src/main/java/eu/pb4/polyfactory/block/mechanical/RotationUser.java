package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface RotationUser extends NetworkComponent.Rotational {
    @Nullable
    static RotationData getNullableRotation(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverWorld) {
            var o = FactoryNodes.ROTATIONAL.getGraphWorld(serverWorld).getNodesAt(pos).findFirst();
            if (o.isPresent()) {
                var ent = o.get().getGraph().getGraphEntity(RotationData.TYPE);
                ent.update(serverWorld);
                return ent;
            }
        }

        return null;
    }

    @Nullable
    static RotationData getNullableRotation(Level world, BlockPos pos, Predicate<NodeHolder<?>> predicate) {
        if (world instanceof ServerLevel serverWorld) {
            var o = FactoryNodes.ROTATIONAL.getGraphWorld(serverWorld).getNodesAt(pos).filter(predicate).findFirst();
            if (o.isPresent()) {
                var ent = o.get().getGraph().getGraphEntity(RotationData.TYPE);
                ent.update(serverWorld);
                return ent;
            }
        }

        return null;
    }

    void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos);

    static RotationData getRotation(Level world, BlockPos pos) {
        var x = getNullableRotation(world, pos);
        if (x != null) {
            return x;
        }
        return RotationData.EMPTY;
    }

    static RotationData getRotation(Level world, BlockPos pos, Predicate<NodeHolder<?>> predicate) {
        var x = getNullableRotation(world, pos, predicate);
        if (x != null) {
            return x;
        }
        return RotationData.EMPTY;
    }
}
