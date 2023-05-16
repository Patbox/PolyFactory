package eu.pb4.polyfactory.display;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.world.World;

public class LodElementHolder extends ElementHolder {
    private int updateTick = 0;

    public boolean isTimeForMediumUpdate() {
        return updateTick % 2 == 0;
    }

    @Override
    public void tick() {
        super.tick();
        this.updateTick++;
    }
}
