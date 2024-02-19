package eu.pb4.polyfactory.block.data.output;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

public class HologramProjectorBlockEntity extends ChanneledDataBlockEntity implements BlockEntityExtraListener {
    private HologramProjectorBlock.Model model;

    private float scale = 2;
    private float offset = 0.2f;
    private float rotationDisplay = 0;
    private boolean forceText = false;

    public HologramProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.HOLOGRAM_PROJECTOR, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putFloat("scale", scale);
        nbt.putFloat("offset", offset);
        nbt.putFloat("rotation_display", rotationDisplay);
        nbt.putBoolean("force_text", forceText);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.scale = nbt.getFloat("scale");
        this.offset = nbt.getFloat("offset");
        this.rotationDisplay = nbt.getFloat("rotation_display");
        this.forceText = nbt.getBoolean("force_text");
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
            this.model.setTransform(scale, offset, rotationDisplay, forceText);
        }
        this.markDirty();
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
            this.model.setTransform(scale, offset, rotationDisplay, forceText);
        }
        this.markDirty();
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
            this.model.setTransform(scale, offset, rotationDisplay, forceText);
        }
        this.markDirty();
    }

    public float offset() {
        return this.offset;
    }

    public void setRotationDisplay(float rotationDisplay) {
        if (this.rotationDisplay == rotationDisplay) {
            return;
        }
        this.rotationDisplay = rotationDisplay;
        if (this.model != null) {
            this.model.setTransform(scale, offset, rotationDisplay, forceText);
        }
        this.markDirty();
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = (HologramProjectorBlock.Model) BlockAwareAttachment.get(chunk, this.pos).holder();
        this.model.setTransform(scale, offset, rotationDisplay, forceText);
        this.model.setData(this.lastData);
    }


    public float rotationDisplay() {
        return this.rotationDisplay;
    }
}
