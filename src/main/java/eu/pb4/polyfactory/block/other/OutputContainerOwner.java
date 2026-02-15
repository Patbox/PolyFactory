package eu.pb4.polyfactory.block.other;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface OutputContainerOwner {
    Container getOwnOutputContainer();
    Container getOutputContainer();

    boolean isOutputConnectedTo(Direction opposite);
}
