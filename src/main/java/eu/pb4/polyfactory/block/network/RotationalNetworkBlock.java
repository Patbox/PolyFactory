package eu.pb4.polyfactory.block.network;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class RotationalNetworkBlock extends NetworkBlock implements NetworkComponent.Rotational {
    protected RotationalNetworkBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(WorldAccess world, BlockPos pos) {
        NetworkComponent.Rotational.updateRotationalAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Rotational;
    }
}