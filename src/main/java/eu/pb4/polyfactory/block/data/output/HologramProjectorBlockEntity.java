package eu.pb4.polyfactory.block.data.output;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putFloat("scale", scale);
        nbt.putFloat("offset", offset);
        nbt.putFloat("pitch", pitch);
        nbt.putFloat("yaw", yaw);
        nbt.putFloat("roll", roll);
        nbt.putBoolean("force_text", forceText);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        this.scale = nbt.getFloat("scale");
        this.offset = nbt.getFloat("offset");
        this.pitch = nbt.getFloat("pitch");
        this.yaw = nbt.getFloat("yaw");
        this.roll = nbt.getFloat("roll");

        if (nbt.contains("rotation_display")) {
            this.roll = nbt.getFloat("rotation_display");
        }

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
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
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
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
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
            this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        }
        this.markDirty();
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
        this.markDirty();
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
        this.markDirty();
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
        this.markDirty();
    }


    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = (HologramProjectorBlock.Model) BlockAwareAttachment.get(chunk, this.pos).holder();
        this.model.setTransform(scale, offset, pitch, yaw, roll, forceText);
        this.model.setData(this.lastData);
    }


}
