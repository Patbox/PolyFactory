package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface RotationUser extends NetworkComponent.Rotational {
    @Nullable
    static RotationData getNullableRotation(World world, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            var o = FactoryNodes.ROTATIONAL.getGraphView(world).getNodesAt(pos).findFirst();
            if (o.isPresent()) {
                var graph = FactoryNodes.ROTATIONAL.getGraphView(world).getGraph(o.get().getGraphId());
                var ent = graph.getGraphEntity(RotationData.TYPE);

                ent.update(serverWorld);

                return ent;
            }
        }

        return null;
    }

    void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos);

    static RotationData getRotation(World world, BlockPos pos) {
        var x = getNullableRotation(world, pos);
        if (x != null) {
            return x;
        }
        return RotationData.EMPTY;
    }
}
