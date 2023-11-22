package eu.pb4.factorytools.api.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomMusicDiscItem extends MusicDiscItem implements SimpleModeledPolymerItem {
    private static final int TRACK_COUNT = 64;
    private static final Map<String, List<Pair<String, Identifier>>> TRACKS = new HashMap<>();

    private final IntSet takenTracks = new IntOpenHashSet();

    private final Identifier musicBase;
    private final Identifier musicFile;

    protected CustomMusicDiscItem(int comparatorOutput, Identifier file, Settings settings, int timeInSeconds) {
        super(comparatorOutput, SoundEvents.INTENTIONALLY_EMPTY, settings, timeInSeconds);
        this.musicBase = new Identifier(file.getNamespace() + "_music_disc", file.getPath());
        this.musicFile = file;
        TRACKS.computeIfAbsent(this.musicBase.getNamespace(), (x) -> new ArrayList<>()).add(new Pair<>(this.musicBase.getPath(), file));
    }

    @Nullable
    public TrackData getFreeTrack() {
        for (int i = 0; i < TRACK_COUNT; i++) {
            if (!this.takenTracks.contains(i)) {
                this.takenTracks.add(i);
                return new TrackData(this, new Identifier(this.musicBase.getNamespace(), this.musicBase.getPath() + "/" + i), i);
            }
        }

        return null;
    }

    public void freeTrack(TrackData data) {
        this.takenTracks.remove(data.value);
    }

    @Override
    public Item getPolymerItem() {
        return Items.MUSIC_DISC_OTHERSIDE;
    }

    @ApiStatus.Internal
    public static void registerResources() {
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((builder) -> {
            for (var namespaced : TRACKS.entrySet()) {
                var base = new JsonObject();

                for (var track : namespaced.getValue()) {
                    var data = new JsonObject();
                    var array = new JsonArray();

                    var sound = new JsonObject();
                    sound.addProperty("name", track.getRight().toString());
                    sound.addProperty("stream", true);

                    array.add(sound);

                    data.add("sounds", array);

                    for (int i = 0; i < TRACK_COUNT; i++) {
                        base.add(track.getLeft() + "/" + i, data);
                    }
                }

                builder.addData("assets/" + namespaced.getKey() + "/sounds.json", base.toString().getBytes(StandardCharsets.UTF_8));
            }
        });
    }


    public record TrackData(CustomMusicDiscItem discItem, Identifier identifier, int value) {
        public SoundEvent soundEvent() {
            return PolymerSoundEvent.of(this.identifier());
        }

        public void free() {
            this.discItem.freeTrack(this);
        }
    }
}
