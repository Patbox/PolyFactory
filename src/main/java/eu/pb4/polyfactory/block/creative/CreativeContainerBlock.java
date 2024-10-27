package eu.pb4.polyfactory.block.creative;

import eu.pb4.polyfactory.block.other.ContainerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CreativeContainerBlock extends ContainerBlock {
    public CreativeContainerBlock(Settings settings) {
        super(999, settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeContainerBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof CreativeContainerBlockEntity be && hit.getSide() == state.get(FACING) && player.isCreative()) {
            be.setItemStack(player.getMainHandStack());
            return ActionResult.SUCCESS_SERVER;
        }

        return ActionResult.PASS;
    }
}
