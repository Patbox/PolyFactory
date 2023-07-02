package eu.pb4.polyfactory.models;

import eu.pb4.polymer.virtualentity.api.ElementHolder;

public class BaseModel extends ElementHolder {
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
