package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.data.WallWithCableBlock;
import eu.pb4.polyfactory.block.fluids.PipeInWallBlock;
import eu.pb4.polyfactory.mixin.util.BlockItemAccessor;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class PipeItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> PipeItem(T block,  Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var state = context.getWorld().getBlockState(context.getBlockPos());
        if (state.getBlock() instanceof WallBlock) {
            var convert = PipeInWallBlock.fromWall(state, context);
            if (convert != null && context.getWorld().setBlockState(context.getBlockPos(), convert)) {
                BlockPos blockPos = context.getBlockPos();
                World world = context.getWorld();
                PlayerEntity playerEntity = context.getPlayer();
                ItemStack itemStack = context.getStack();
                BlockState blockState2 = world.getBlockState(blockPos);
                if (blockState2.isOf(convert.getBlock())) {
                    blockState2 = ((BlockItemAccessor) this).callPlaceFromNbt(blockPos, world, itemStack, blockState2);
                    this.postPlacement(blockPos, world, playerEntity, itemStack, blockState2);
                    BlockItemAccessor.callCopyComponentsToBlockEntity(world, blockPos, itemStack);
                    blockState2.getBlock().onPlaced(world, blockPos, blockState2, playerEntity, itemStack);
                    if (playerEntity instanceof ServerPlayerEntity) {
                        Criteria.PLACED_BLOCK.trigger((ServerPlayerEntity)playerEntity, blockPos, itemStack);
                    }
                }

                BlockSoundGroup blockSoundGroup = blockState2.getSoundGroup();
                world.playSound(null, blockPos, this.getPlaceSound(this.getBlock().getDefaultState()), SoundCategory.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
                world.emitGameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Emitter.of(playerEntity, blockState2));
                itemStack.decrementUnlessCreative(1, playerEntity);
                return ActionResult.SUCCESS_SERVER;
            }
        }

        return super.useOnBlock(context);
    }
}
