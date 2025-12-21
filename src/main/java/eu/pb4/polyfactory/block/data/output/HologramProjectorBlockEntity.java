package eu.pb4.polyfactory.block.data.output;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class HologramProjectorBlockEntity extends ChanneledDataBlockEntity implements BlockEntityExtraListener {
    private HologramProjectorBlock.Model model;

    private float scale = 2;
    private float offset = 0.2f;
    private float pitch = 0;
    private float yaw = 0;
    private float roll = 0;
    private boolean forceText = false;

    public HologramProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.HOLOGRAM_PROJECTOR, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putFloat("scale", scale);
        view.putFloat("offset", offset);
        view.putFloat("pitch", pitch);
        view.putFloat("yaw", yaw);
        view.putFloat("roll", roll);
        view.putBoolean("force_text", forceText);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.scale = view.getFloatOr("scale", 1);
        this.offset = view.getFloatOr("offset", 0.5f);
        this.pitch = view.getFloatOr("pitch", 0);
        this.yaw = view.getFloatOr("yaw", 0);
        this.roll = view.getFloatOr("roll", view.getFloatOr("rotation_display", 0));
        this.forceText = view.getBooleanOr("force_text", false);
    }

    @Override
    public void setCachedData(DataContainer lastData) {
        super.setCachedData(lastData);
        if (this.model != null) {
            this.model.setData(this.lastData);
        }
    }

    public void setScale(float scale) {
        if (this.scale == scale) {
            return;
        }
        this.scale = scale;
        if (this.model != null) {
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        }
        this.setChanged();
    }

    public boolean forceText() {
        return this.forceText;
    }

    public void setForceText(boolean forceText) {
        if (this.forceText == forceText) {
            return;
        }
        this.forceText = forceText;
        if (this.model != null) {
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        }
        this.setChanged();
    }

    public float scale() {
        return this.scale;
    }

    public void setOffset(float offset) {
        if (this.offset == offset) {
            return;
        }
        this.offset = offset;
        if (this.model != null) {
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        }
        this.setChanged();
    }

    public float offset() {
        return this.offset;
    }

    public float pitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        if (this.pitch == pitch) {
            return;
        }
        this.pitch = pitch;
        if (this.model != null) {
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        }
        this.setChanged();
    }

    public float yaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        if (this.yaw == yaw) {
            return;
        }
        this.yaw = yaw;
        if (this.model != null) {
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        }
        this.setChanged();
    }

    public float roll() {
        return this.roll;
    }

    public void setRoll(float roll) {
        if (this.roll == roll) {
            return;
        }
        this.roll = roll;
        if (this.model != null) {
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        }
        this.setChanged();
    }


    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = (HologramProjectorBlock.Model) BlockAwareAttachment.get(chunk, this.worldPosition).holder();
        this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        this.model.setData(this.lastData);
    }


}
