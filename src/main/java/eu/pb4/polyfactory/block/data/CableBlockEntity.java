package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class CableBlockEntity extends BlockEntity implements BlockEntityExtraListener {
    private int color = -2;
    @Nullable
    private CableBlock.Model model;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CABLE, pos, state);
    }


    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("color", this.color);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        setColor(nbt.getInt("color"));
    }

    public void setColor(int color) {
        if (this.color != color) {
            this.color = color;
            if (this.model != null) {
                this.model.setColor(color);
            }
            this.markDirty();
        }
    }

    public int getColor() {
        return this.color;
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = (CableBlock.Model) BlockBoundAttachment.get(chunk, this.getPos()).holder();
        this.model.setColor(this.color);
    }
}
