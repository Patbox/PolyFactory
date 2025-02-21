package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.data.CapacityData;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.SpeakerNode;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecordPlayerBlockEntity extends ChanneledDataBlockEntity implements SingleStackInventory, ChanneledDataCache, BlockEntityExtraListener {
    private final List<Vec3d> speakers = new ArrayList<>();
    private ItemStack stack = ItemStack.EMPTY;
    private long ticksSinceSongStarted = 0;
    @Nullable
    private RegistryEntry<JukeboxSong> song = null;
    private RecordPlayerBlock.Model model;
    private float volume = 1;

    public RecordPlayerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(FactoryBlockEntities.RECORD_PLAYER, blockPos, blockState);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        if (world instanceof ServerWorld serverWorld && t instanceof RecordPlayerBlockEntity be) {
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
        var song = stack.get(DataComponentTypes.JUKEBOX_PLAYABLE);
        this.stopPlaying();
        if (song != null) {
            this.startPlaying(song.song().getEntry(this.world.getRegistryManager()).get());
        } else {
            DataProvider.sendData(this.world, pos, CapacityData.ZERO);
        }
        this.markDirty();
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        var newStack = ItemStack.fromNbtOrEmpty(lookup, nbt.getCompound("stack"));
        if (!ItemStack.areItemsAndComponentsEqual(newStack, this.stack) || newStack.isEmpty()) {
            this.stopPlaying();
        }
        this.stack = newStack;

        if (nbt.contains("ticks_since_song_started", NbtElement.NUMBER_TYPE)) {
            JukeboxSong.getSongEntryFromStack(lookup, this.stack).ifPresent((song) -> {
                this.setValues(song, nbt.getLong("ticks_since_song_started"));
            });
        }
        if (nbt.contains("volume", NbtElement.NUMBER_TYPE)) {
            this.volume = nbt.getFloat("volume");
        } else {
            this.volume = 1;
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.put("stack", this.stack.encodeAllowEmpty(lookup));
        if (this.song != null) {
            nbt.putLong("ticks_since_song_started", this.ticksSinceSongStarted);
        }
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.stack.isEmpty() && stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE);
    }

    @Override
    public boolean canTransferTo(Inventory hopperInventory, int slot, ItemStack stack) {
        return hopperInventory.containsAny(ItemStack::isEmpty);
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    private void tick(ServerWorld serverWorld, BlockPos pos, BlockState state) {
        if (this.song != null) {
            if (this.song.value().shouldStopPlaying(this.ticksSinceSongStarted)) {
                DataProvider.sendData(serverWorld, pos, new CapacityData(this.song.value().getLengthInTicks(), this.song.value().getLengthInTicks()));
                this.stopPlaying();
            } else {
                DataProvider.sendData(serverWorld, pos, new CapacityData(this.ticksSinceSongStarted, this.song.value().getLengthInTicks()));
                var nodes = FactoryNodes.DATA.getGraphWorld(serverWorld).getNodesAt(pos).findFirst();
                if (nodes.isEmpty()) {
                    ++this.ticksSinceSongStarted;
                    return;
                }

                var speakers = nodes.get().getGraph().getCachedNodes(SpeakerNode.CACHE);
                this.speakers.clear();
                for (var speaker : speakers) {
                    if (speaker.getNode().channel() == this.channel()) {
                        this.speakers.add(Vec3d.ofCenter(speaker.getBlockPos()).offset(speaker.getNode().facing(), 0.6));
                    }
                }

                if (this.hasSecondPassed()) {
                    serverWorld.emitGameEvent(GameEvent.JUKEBOX_PLAY, this.pos, GameEvent.Emitter.of(state));
                    float f = (float) serverWorld.getRandom().nextInt(4) / 24.0F;
                    for (var speaker : this.speakers) {
                        serverWorld.spawnParticles(ParticleTypes.NOTE, speaker.x, speaker.y, speaker.z, 0, f, 0.0, 0.0, 1.0);
                    }
                }
                if (this.model != null) {
                    this.model.updatePosition(this.speakers, this.volume);
                }
                ++this.ticksSinceSongStarted;
            }
        }
    }

    public void setValues(RegistryEntry<JukeboxSong> song, long ticksPlaying) {
        if (!song.value().shouldStopPlaying(ticksPlaying)) {
            this.song = song;
            this.ticksSinceSongStarted = ticksPlaying;
        }
    }

    public void startPlaying(RegistryEntry<JukeboxSong> song) {
        this.song = song;
        this.ticksSinceSongStarted = 0L;
        assert this.world != null;
        DataProvider.sendData(this.world, pos, new CapacityData(0, this.song.value().getLengthInTicks()));
        if (this.model != null) {
            this.model.startPlaying(song.value().soundEvent(), this.speakers, this.volume);
        }
        this.markDirty();
    }

    public void stopPlaying() {
        if (this.song != null) {
            this.song = null;
            this.ticksSinceSongStarted = 0L;
            if (this.model != null) {
                this.model.stopPlaying();
            }
            this.markDirty();
        }
    }

    private boolean hasSecondPassed() {
        return this.ticksSinceSongStarted % 20L == 0L;
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = (RecordPlayerBlock.Model) Objects.requireNonNull(BlockBoundAttachment.get(chunk, this.pos)).holder();
        super.onListenerUpdate(chunk);
    }
}
