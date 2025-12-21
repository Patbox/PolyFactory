package eu.pb4.polyfactory.block.mechanical.source;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HandCrankBlockEntity extends BlockEntity {
    public long lastTick = -1;
    public boolean negative;

    public HandCrankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.HAND_CRANK, pos, state);
    }
}
