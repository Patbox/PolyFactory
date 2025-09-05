package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.ListData;
import eu.pb4.polyfactory.data.SoundEventData;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.nodes.data.SpeakerNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SpeakerBlock extends DirectionalCabledDataBlock implements DataReceiver {
    public SpeakerBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SpeakerNode(state.get(FACING), getDirections(state), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        if (data.isEmpty()) {
            return false;
        }

        final var x = BlockAwareAttachment.get(world, selfPos);
        if (x == null || !(x.holder() instanceof Model model)) {
            return false;
        }
        if (data instanceof ListData listData) {
            return listData.forEachFlatBool(d -> playNote(d, model, selfPos, selfState, world));
        } else {
            return playNote(data, model, selfPos, selfState, world);
        }
    }

    private boolean playNote(DataContainer data, Model model, BlockPos selfPos, BlockState selfState, ServerWorld world)  {
        var soundEvent = Registries.SOUND_EVENT.getEntry(SoundEvents.INTENTIONALLY_EMPTY);
        var volume = 1f;
        var pitch = 1f;
        var seed = 0L;
        if (data instanceof SoundEventData soundEventData) {
            soundEvent = soundEventData.soundEvent();
            volume = soundEventData.volume();
            pitch = soundEventData.pitch();
            seed = soundEventData.seed();
        } else {
            var notFound = true;
            var parts = data.asString().split(" ", 4);
            if (parts.length != 0) {
                try {
                    var instrument = NoteBlockInstrument.valueOf(parts[0].toUpperCase(Locale.ROOT));
                    soundEvent = instrument.getSound();
                    notFound = false;
                    if (parts.length >= 2) {
                        try {
                            pitch = Float.parseFloat(parts[1]);
                        } catch (Throwable ignored) {
                        }
                    }

                    if (parts.length >= 3) {
                        try {
                            volume = Float.parseFloat(parts[2]);
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {}
            }

            if (notFound) {
                return false;
            }
        }

        model.playSound(soundEvent, Math.min(volume, 1.5f), pitch, seed);

        var facing = Vec3d.ofCenter(selfPos).offset(selfState.get(FACING), 0.6);
        world.spawnParticles(ParticleTypes.NOTE, facing.x, facing.y, facing.z, 0, pitch / 24, 0, 0, 1);

        return true;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends DirectionalCabledDataBlock.Model {
        private final ItemDisplayElement soundSource = new ItemDisplayElement();
        protected Model(BlockState state) {
            super(state);
            this.soundSource.setInvisible(true);
            this.updateStatePos(state);
            this.addElement(this.soundSource);
        }

        @Override
        protected void updateStatePos(BlockState state) {
            super.updateStatePos(state);
            if (this.soundSource != null) {
                this.soundSource.setOffset(state.get(FACING).getDoubleVector().multiply(0.5));
            }
        }

        public void playSound(RegistryEntry<SoundEvent> sound, float volume, float pitch, long seed) {
            this.sendPacket(VirtualEntityUtils.createPlaySoundFromEntityPacket(this.soundSource.getEntityId(), sound, SoundCategory.RECORDS, volume, pitch, seed));
        }
    }
}
