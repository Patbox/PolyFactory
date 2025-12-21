package eu.pb4.polyfactory.block.fluids.transport;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PumpBlockEntity extends PipeLikeBlockEntity {
    private double speed;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PUMP, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putDouble("speed", this.speed);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.speed = view.getDoubleOr("speed", 0);
    }

    @Override
    protected boolean hasDirection(Direction direction) {
        return direction.getAxis() == this.getBlockState().getValue(PumpBlock.FACING).getAxis();
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PumpBlockEntity pump)) {
            return;
        }
        pump.preTick();

        var speed = RotationUser.getRotation(world, pos).speed();
        pump.speed = speed;
        var strength = speed / 30 / 20;
        NetworkComponent.Pipe.forEachLogic((ServerLevel) world, pos, l -> l.setSourceStrength(pos, strength));

        if (speed != 0) {
            var dir = state.getValue(PumpBlock.FACING);
            if (pump.container.isNotEmpty()) {
                pump.pushFluid(dir, strength);
            }
            if (pump.container.isNotFull()) {
                pump.pullFluid(dir.getOpposite(), strength);
            }
        }
        pump.postTick();
    }
}
