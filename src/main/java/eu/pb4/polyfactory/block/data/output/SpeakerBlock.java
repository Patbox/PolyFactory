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
import net.minecraft.world.level.block.NoteBlock;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;

public class SpeakerBlock extends DirectionalCabledDataBlock implements DataReceiver {
    public SpeakerBlock(Properties settings) {
        super(settings);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SpeakerNode(state.getValue(FACING), getDirections(state), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir, int dataId) {
        if (data.isEmpty()) {
            return false;
        }

        final var x = BlockAwareAttachment.get(world, selfPos);
        if (x == null || !(x.holder() instanceof SpeakerBlock.Model model)) {
            return false;
        }
        if (data instanceof ListData listData) {
            return listData.forEachFlatBool(d -> playNote(d, model, selfPos, selfState, world));
        } else {
            return playNote(data, model, selfPos, selfState, world);
        }
    }

    private boolean playNote(DataContainer data, SpeakerBlock.Model model, BlockPos selfPos, BlockState selfState, ServerLevel world)  {
        var soundEvent = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY);
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
                    soundEvent = instrument.getSoundEvent();
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
                parts = data.asString().split(":", 3);
                if (parts.length == 3) {
                    try {
                        var instrument = NoteBlockInstrument.values()[Integer.parseInt(parts[2])];
                        soundEvent = instrument.getSoundEvent();
                        notFound = false;
                        pitch = NoteBlock.getPitchFromNote(Integer.parseInt(parts[1]));
                        volume = 0.5f;
                    } catch (Throwable ignored) {}
                }
            }

            if (notFound) {
                return false;
            }
        }

        model.playSound(soundEvent, Math.min(volume, 1.5f), pitch, seed);

        var tick = world.getGameTime();
        if (model.particleTick != tick) {
            model.particleTick = tick;
            var facing = Vec3.atCenterOf(selfPos).relative(selfState.getValue(FACING), 0.6);
            world.sendParticles(ParticleTypes.NOTE, facing.x, facing.y, facing.z, 0, pitch / 24, 0, 0, 1);
        }

        return true;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new SpeakerBlock.Model(initialBlockState);
    }

    public static class Model extends DirectionalCabledDataBlock.Model {
        private final ItemDisplayElement soundSource = new ItemDisplayElement();
        protected long particleTick = -1;

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
                this.soundSource.setOffset(state.getValue(FACING).getUnitVec3().scale(0.5));
            }
        }

        public void playSound(Holder<SoundEvent> sound, float volume, float pitch, long seed) {
            this.sendPacket(VirtualEntityUtils.createPlaySoundFromEntityPacket(this.soundSource.getEntityId(), sound, SoundSource.RECORDS, volume, pitch, seed));
        }
    }
}
