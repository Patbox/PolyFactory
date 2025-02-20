package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.SoundEventData;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverSelectiveSideNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.nodes.data.SpeakerNode;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SpeakerBlock extends GenericCabledDataBlock implements DataReceiver {
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

        var soundEvent = Registries.SOUND_EVENT.getEntry(SoundEvents.INTENTIONALLY_EMPTY);
        var volume = 1f;
        var pitch = 1f;
        if (data instanceof SoundEventData soundEventData) {
            soundEvent = soundEventData.soundEvent();
            volume = soundEventData.volume();
            pitch = soundEventData.pitch();
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
                //soundEvent = Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_ITEM_PICKUP);
                //pitch = (float) Math.abs((data.asDouble() / 10) % 1.5 + 0.5);
                return false;
            }
        }

        var facing = Vec3d.ofCenter(selfPos).offset(selfState.get(FACING), 0.6);
        world.playSound(null, facing.x, facing.y, facing.z, soundEvent, SoundCategory.RECORDS, Math.min(volume, 1.5f), pitch);
        world.spawnParticles(ParticleTypes.NOTE, facing.x, facing.y, facing.z, 0, pitch / 24, 0, 0, 1);
        return true;
    }
}
