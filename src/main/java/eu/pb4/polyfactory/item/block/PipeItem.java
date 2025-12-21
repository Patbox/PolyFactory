package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.fluids.transport.PipeInWallBlock;
import eu.pb4.polyfactory.mixin.util.BlockItemAccessor;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class PipeItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> PipeItem(T block,  Properties settings) {
        super(block, settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && !context.getPlayer().mayBuild()) {
            return InteractionResult.FAIL;
        }
        var state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof WallBlock) {
            var convert = PipeInWallBlock.fromWall(state, context);
            if (convert != null && context.getLevel().setBlockAndUpdate(context.getClickedPos(), convert)) {
                BlockPos blockPos = context.getClickedPos();
                Level world = context.getLevel();
                Player playerEntity = context.getPlayer();
                ItemStack itemStack = context.getItemInHand();
                BlockState blockState2 = world.getBlockState(blockPos);
                if (blockState2.is(convert.getBlock())) {
                    blockState2 = ((BlockItemAccessor) this).callUpdateBlockStateFromTag(blockPos, world, itemStack, blockState2);
                    this.updateCustomBlockEntityTag(blockPos, world, playerEntity, itemStack, blockState2);
                    BlockItemAccessor.callUpdateBlockEntityComponents(world, blockPos, itemStack);
                    blockState2.getBlock().setPlacedBy(world, blockPos, blockState2, playerEntity, itemStack);
                    if (playerEntity instanceof ServerPlayer) {
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)playerEntity, blockPos, itemStack);
                    }
                }

                SoundType blockSoundGroup = blockState2.getSoundType();
                world.playSound(null, blockPos, this.getPlaceSound(this.getBlock().defaultBlockState()), SoundSource.BLOCKS, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F);
                world.gameEvent(GameEvent.BLOCK_PLACE, blockPos, GameEvent.Context.of(playerEntity, blockState2));
                itemStack.consume(1, playerEntity);
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return super.useOn(context);
    }
}
