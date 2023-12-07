package eu.pb4.factorytools.api.item;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.sound.SoundEvent;

public class PolymerMusicDiscItem extends MusicDiscItem implements AutoModeledPolymerItem {
    public PolymerMusicDiscItem(int comparatorOutput, SoundEvent event, Item.Settings settings, int timeInSeconds) {
        super(comparatorOutput, event, settings, timeInSeconds);
    }
    @Override
    public Item getPolymerItem() {
        return Items.MUSIC_DISC_OTHERSIDE;
    }
}
