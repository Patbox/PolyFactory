package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RecordPlayerBlock extends CabledDataProviderBlock {
    public RecordPlayerBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RecordPlayerBlockEntity(pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        world.updateComparators(pos, this);
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (state.get(FACING).getOpposite() != hit.getSide() && world.getBlockEntity(pos) instanceof RecordPlayerBlockEntity be) {
            if (be.getStack().isEmpty() && stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)) {
                be.setStack(stack.copyWithCount(1));
                stack.decrementUnlessCreative(1, player);
                return ActionResult.SUCCESS_SERVER;
            } else if (!be.getStack().isEmpty()) {
                var newStack = be.getStack().copy();
                be.setStack(ItemStack.EMPTY);
                if (stack.isEmpty()) {
                    player.setStackInHand(hand, newStack);
                } else {
                    player.giveOrDropStack(newStack);
                }
                return ActionResult.SUCCESS_SERVER;
            }
        }

        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.isSneaking() && state.get(FACING).getOpposite() != hit.getSide() && world.getBlockEntity(pos) instanceof RecordPlayerBlockEntity be) {
            var stack = be.getStack().copy();
            if (stack.isEmpty()) {
                return ActionResult.FAIL;
            }
            be.setStack(ItemStack.EMPTY);
            if (player.getMainHandStack().isEmpty()) {
                player.setStackInHand(Hand.MAIN_HAND, stack);
            } else {
                player.giveOrDropStack(stack);
            }
            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return RecordPlayerBlockEntity::ticker;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static final class Model extends GenericCabledDataBlock.Model {
        private final ItemDisplayElement soundSource = new ItemDisplayElement();
        private boolean isPlaying = false;
        private Model(BlockState state) {
            super(state);
            this.soundSource.setOffset(new Vec3d(0, Integer.MAX_VALUE, 0));
            this.soundSource.setInvisible(true);
            //this.soundSource.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
            //this.soundSource.setItem(Items.MUSIC_DISC_5.getDefaultStack());
        }

        public void stopPlaying() {
            this.isPlaying = false;
            this.removeElement(this.soundSource);
        }

        public void startPlaying(RegistryEntry<SoundEvent> event, List<Vec3d> speakers, float volume) {
            this.isPlaying = true;
            this.addElement(this.soundSource);
            this.updatePosition(speakers, volume);
            this.sendPacket(VirtualEntityUtils.createPlaySoundFromEntityPacket(this.soundSource.getEntityId(), event, SoundCategory.RECORDS, 4F + 1 / 16f, 1, 0));
        }

        public void updatePosition(List<Vec3d> speakers, float volume) {
            if (!this.isPlaying) {
                return;
            }
            for (var handler : this.getWatchingPlayers()) {

                var player = handler.player.getEyePos();
                Vec3d closest = this.soundSource.getCurrentPos();
                //double x = 0, y = 0, z = 0, weight = 0;
                if (volume > 0.01) {
                    var distance = Double.MAX_VALUE;
                    for (var speaker : speakers) {
                        /*var d = player.squaredDistanceTo(speaker);
                        x += (speaker.x - player.x) / d;
                        y += (speaker.y - player.y) / d;
                        z += (speaker.z - player.z) / d;
                        weight += 1 / d;*/
                        var d = player.squaredDistanceTo(speaker);
                        if (d < distance) {
                            distance = d;
                            closest = speaker;
                        }
                    }
                    //closest = player.add(x / weight, y / weight, z / weight);
                }

                handler.sendPacket(new EntityPositionSyncS2CPacket(this.soundSource.getEntityId(),
                        new PlayerPosition(closest.add(0, Math.signum(closest.y - player.y) * (16 * 4 * (1 - volume) + 1), 0), Vec3d.ZERO, 0, 0), false));
            }
        }
    }
}
