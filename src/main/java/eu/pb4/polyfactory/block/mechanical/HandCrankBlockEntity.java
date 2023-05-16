package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public class HandCrankBlockEntity extends BlockEntity {
    public long lastTick = -1;
    public HandCrankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.HAND_CRANK, pos, state);
    }
}
