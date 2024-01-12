package eu.pb4.polyfactory.models;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;

import java.util.function.Predicate;

public class RotationAwareModel extends BlockModel {
    public final int getUpdateRate() {
        return RotationConstants.VISUAL_UPDATE_RATE;
    }

    public final RotationData getRotationData() {

        var block = this.blockAware();
        if (block != null) {
            return RotationUser.getRotation(block.getWorld(), block.getBlockPos());
        }
        return RotationData.EMPTY;
    }

    public final float getRotation() {
        return getRotationData().rotation();
    }

    public final RotationData getRotationData(Predicate<NodeHolder<?>> predicate) {
        if (this.inWorld()) {
            var block = this.blockAware();
            if (block != null) {
                return RotationUser.getRotation(block.getWorld(), block.getBlockPos(), predicate);
            }
        }
        return RotationData.EMPTY;
    }
}
