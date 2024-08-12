package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.util.DebugTextProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PipeBlockEntity extends PipeLikeBlockEntity {
    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PIPE, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PipeBlockEntity pipe)) {
            return;
        }
        pipe.preTick();
        if (pipe.container.isNotEmpty()) {
            NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPushFlows(pos, pipe.container::isNotEmpty, pipe::pushFluid);
        }
        if (pipe.container.isNotFull()) {
            NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPullFlows(pos, pipe.container::isNotFull, pipe::pullFluid);
        }
        pipe.postTick();
    }

    @Override
    protected boolean hasDirection(Direction direction) {
        return ((PipeBaseBlock) this.getCachedState().getBlock()).checkModelDirection(this.getCachedState(), direction);
    }
}
