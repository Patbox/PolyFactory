package eu.pb4.polyfactory.models;

import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;

public class RotationAwareModel extends BaseModel {


    public final int getUpdateRate() {
        return RotationConstants.VISUAL_UPDATE_RATE;
    }

    public final RotationData getRotationData() {

        var block = BlockBoundAttachment.get(this);
        if (block != null) {
            return RotationUser.getRotation(block.getWorld(), block.getBlockPos());
        }
        return RotationData.EMPTY;
    }

    public final float getRotation() {
        return getRotationData().rotation();
    }
}
