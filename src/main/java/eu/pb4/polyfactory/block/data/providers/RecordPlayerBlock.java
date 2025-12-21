package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RecordPlayerBlock extends DirectionalCabledDataProviderBlock {
    public RecordPlayerBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecordPlayerBlockEntity(pos, state);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(FACING).getOpposite() != hit.getDirection() && world.getBlockEntity(pos) instanceof RecordPlayerBlockEntity be) {
            if (be.getStack().isEmpty() && stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
                be.setStack(stack.copyWithCount(1));
                stack.consume(1, player);
                return InteractionResult.SUCCESS_SERVER;
            } else if (!be.getStack().isEmpty()) {
                var newStack = be.getStack().copy();
                be.setStack(ItemStack.EMPTY);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, newStack);
                } else {
                    player.handleExtraItemsCreatedOnUse(newStack);
                }
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && state.getValue(FACING).getOpposite() != hit.getDirection() && world.getBlockEntity(pos) instanceof RecordPlayerBlockEntity be) {
            var stack = be.getStack().copy();
            if (stack.isEmpty()) {
                return InteractionResult.FAIL;
            }
            be.setStack(ItemStack.EMPTY);
            if (player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            } else {
                player.handleExtraItemsCreatedOnUse(stack);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return RecordPlayerBlockEntity::ticker;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.data.providers.RecordPlayerBlock.Model(initialBlockState);
    }

    public static final class Model extends DirectionalCabledDataBlock.Model {
        private final ItemDisplayElement soundSource = new ItemDisplayElement();
        private boolean isPlaying = false;
        private Model(BlockState state) {
            super(state);
            this.soundSource.setOffset(new Vec3(0, Integer.MAX_VALUE, 0));
            this.soundSource.setInvisible(true);
            //this.soundSource.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
            //this.soundSource.setItem(Items.MUSIC_DISC_5.getDefaultStack());
        }

        public void stopPlaying() {
            this.isPlaying = false;
            this.removeElement(this.soundSource);
        }

        public void startPlaying() {
            this.isPlaying = true;
            this.addElement(this.soundSource);
        }

        public void playSoundIfActive(Holder<SoundEvent> event) {
            this.sendPacket(VirtualEntityUtils.createPlaySoundFromEntityPacket(this.soundSource.getEntityId(), event, SoundSource.RECORDS, 4F + 1 / 16f, 1, 0));
        }


        public void updatePosition(List<Vec3> speakers, float volume) {
            if (!this.isPlaying) {
                return;
            }
            for (var handler : this.getWatchingPlayers()) {

                var player = handler.player.getEyePosition();
                Vec3 closest = this.soundSource.getCurrentPos();
                //double x = 0, y = 0, z = 0, weight = 0;
                if (volume > 0.01) {
                    var distance = Double.MAX_VALUE;
                    for (var speaker : speakers) {
                        /*var d = player.squaredDistanceTo(speaker);
                        x += (speaker.x - player.x) / d;
                        y += (speaker.y - player.y) / d;
                        z += (speaker.z - player.z) / d;
                        weight += 1 / d;*/
                        var d = player.distanceToSqr(speaker);
                        if (d < distance) {
                            distance = d;
                            closest = speaker;
                        }
                    }
                    //closest = player.add(x / weight, y / weight, z / weight);
                }

                handler.send(new ClientboundEntityPositionSyncPacket(this.soundSource.getEntityId(),
                        new PositionMoveRotation(closest.add(0, Math.signum(closest.y - player.y) * (16 * 4 * (1 - volume) + 1), 0), Vec3.ZERO, 0, 0), false));
            }
        }
    }
}
