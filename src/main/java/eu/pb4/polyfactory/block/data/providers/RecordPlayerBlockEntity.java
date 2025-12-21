package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.data.CapacityData;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.SpeakerNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SingleStackContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class RecordPlayerBlockEntity extends ChanneledDataBlockEntity implements SingleStackContainer, ChanneledDataCache, BlockEntityExtraListener {
    private final List<Vec3> speakers = new ArrayList<>();
    private ItemStack stack = ItemStack.EMPTY;
    private long ticksSinceSongStarted = 0;
    @Nullable
    private Holder<JukeboxSong> song = null;
    private RecordPlayerBlock.Model model;
    private float volume = 1;

    public RecordPlayerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(FactoryBlockEntities.RECORD_PLAYER, blockPos, blockState);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        if (world instanceof ServerLevel serverWorld && t instanceof RecordPlayerBlockEntity be) {
            be.tick(serverWorld, pos, state);
        }
    }

    @Override
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
        var song = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        this.stopPlaying();
        if (song != null) {
            this.startPlaying(song.song().unwrap(this.level.registryAccess()).get());
        } else {
            DataProvider.sendData(this.level, worldPosition, CapacityData.ZERO);
        }
        this.setChanged();
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        var newStack = view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        if (!ItemStack.isSameItemSameComponents(newStack, this.stack) || newStack.isEmpty()) {
            this.stopPlaying();
        }
        this.stack = newStack;

        if (view.getIntOr("ticks_since_song_started", -999) != -999) {
            JukeboxSong.fromStack(view.lookup(), this.stack).ifPresent((song) -> {
                this.setValues(song, view.getLongOr("ticks_since_song_started", 0));
            });
        }
        if (view.getFloatOr("volume", Float.POSITIVE_INFINITY) != Float.POSITIVE_INFINITY) {
            this.volume = view.getFloatOr("volume", 0);
        } else {
            this.volume = 1;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (!this.stack.isEmpty()) {
            view.store("stack", ItemStack.OPTIONAL_CODEC, this.stack);
        }
        if (this.song != null) {
            view.putLong("ticks_since_song_started", this.ticksSinceSongStarted);
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.stack.isEmpty() && stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }

    @Override
    public boolean canTakeItem(Container hopperInventory, int slot, ItemStack stack) {
        return hopperInventory.hasAnyMatching(ItemStack::isEmpty);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    private void tick(ServerLevel serverWorld, BlockPos pos, BlockState state) {
        if (this.song != null) {
            if (this.song.value().hasFinished(this.ticksSinceSongStarted)) {
                DataProvider.sendData(serverWorld, pos, new CapacityData(this.song.value().lengthInTicks(), this.song.value().lengthInTicks()));
                this.stopPlaying();
            } else {
                DataProvider.sendData(serverWorld, pos, new CapacityData(this.ticksSinceSongStarted, this.song.value().lengthInTicks()));
                var nodes = FactoryNodes.DATA.getGraphWorld(serverWorld).getNodesAt(pos).findFirst();
                if (nodes.isEmpty()) {
                    ++this.ticksSinceSongStarted;
                    return;
                }

                var speakers = nodes.get().getGraph().getCachedNodes(SpeakerNode.CACHE);
                this.speakers.clear();
                for (var speaker : speakers) {
                    if (speaker.getNode().channel() == this.channel()) {
                        this.speakers.add(Vec3.atCenterOf(speaker.getBlockPos()).relative(speaker.getNode().facing(), 0.6));
                    }
                }

                if (this.hasSecondPassed()) {
                    serverWorld.gameEvent(GameEvent.JUKEBOX_PLAY, this.worldPosition, GameEvent.Context.of(state));
                    float f = (float) serverWorld.getRandom().nextInt(4) / 24.0F;
                    for (var speaker : this.speakers) {
                        serverWorld.sendParticles(ParticleTypes.NOTE, speaker.x, speaker.y, speaker.z, 0, f, 0.0, 0.0, 1.0);
                    }
                }
                if (this.model != null) {
                    this.model.updatePosition(this.speakers, this.volume);
                }
                if (!this.speakers.isEmpty()) {
                    if (this.ticksSinceSongStarted == 5 && FactoryUtil.getClosestPlayer(level, pos, 32) instanceof ServerPlayer player) {
                        TriggerCriterion.trigger(player, FactoryTriggers.CONNECT_RECORD_PLAYER_AND_SPEAKERS);
                    }
                }
                ++this.ticksSinceSongStarted;
            }
        }
    }

    public void setValues(Holder<JukeboxSong> song, long ticksPlaying) {
        if (!song.value().hasFinished(ticksPlaying)) {
            this.song = song;
            this.ticksSinceSongStarted = ticksPlaying;
        }
    }

    public void startPlaying(Holder<JukeboxSong> song) {
        this.song = song;
        this.ticksSinceSongStarted = 0L;
        assert this.level != null;
        DataProvider.sendData(this.level, worldPosition, new CapacityData(0, this.song.value().lengthInTicks()));
        if (this.model != null) {
            this.model.startPlaying();
            this.model.updatePosition(this.speakers, this.volume);
            this.model.playSoundIfActive(song.value().soundEvent());
        }
        this.setChanged();
    }

    public void stopPlaying() {
        if (this.song != null) {
            this.song = null;
            this.ticksSinceSongStarted = 0L;
            if (this.model != null) {
                this.model.stopPlaying();
            }
            this.setChanged();
        }
    }

    private boolean hasSecondPassed() {
        return this.ticksSinceSongStarted % 20L == 0L;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = (RecordPlayerBlock.Model) Objects.requireNonNull(BlockBoundAttachment.get(chunk, this.worldPosition)).holder();
        super.onListenerUpdate(chunk);
    }
}
