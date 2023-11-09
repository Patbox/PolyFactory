package eu.pb4.factorytools.api.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface AttackableBlock {
    ActionResult onPlayerAttack(BlockState state, PlayerEntity player, World world, BlockPos pos, Direction direction);
}
