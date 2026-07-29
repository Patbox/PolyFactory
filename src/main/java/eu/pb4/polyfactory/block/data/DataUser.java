package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public interface DataUser extends NetworkComponent.Data {
    static Component NETWORK_NAME_OUTPUT = Component.translatable("text.polyfactory.network.output");
    static Component NETWORK_NAME_INPUT = Component.translatable("text.polyfactory.network.input");
    static Component NETWORK_NAME_INPUT_A = Component.translatable("text.polyfactory.network.input_a");
    static Component NETWORK_NAME_INPUT_B = Component.translatable("text.polyfactory.network.input_b");
    static Component NETWORK_NAME_INPUT_A_OUTPUT = Component.translatable("text.polyfactory.network.input_a_output");
    static Component NETWORK_NAME_INPUT_B_OUTPUT = Component.translatable("text.polyfactory.network.input_b_output");

    @Nullable
    default Component getDataNetworkName(ServerLevel level, BlockPos blockPos, Vec3 location, BlockState blockState, BlockEntity entity) {
        return null;
    }

    default BlockPos offsetDataReadingPosition(BlockPos pos, BlockState state) {
        return pos;
    }

    default Predicate<NodeHolder<BlockNode>> getDataReadingNodePredicate(ServerLevel level, BlockPos blockPos, Vec3 location, BlockState blockState, BlockEntity entity) {
        return _ -> true;
    }
}
