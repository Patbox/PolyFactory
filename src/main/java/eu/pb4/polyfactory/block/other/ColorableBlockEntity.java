package eu.pb4.polyfactory.block.other;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class ColorableBlockEntity extends BlockEntity implements BlockEntityExtraListener, ColorProvider {
    private int color = -2;
    @Nullable
    private ColorProvider.Consumer model;

    public ColorableBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.COLOR_CONTAINER, pos, state);
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

    @Override
    public void setColor(int color) {
        if (this.color != color) {
            this.color = color;
            if (this.model != null) {
                this.model.setColor(color);
            }
            this.markDirty();
        }
    }

    @Override

    public int getColor() {
        return this.color;
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = (ColorProvider.Consumer) BlockBoundAttachment.get(chunk, this.getPos()).holder();
        this.model.setColor(this.color);
    }

    @Override
    public boolean isDefaultColor() {
        return this.color == 0xFFFFFF;
    }
}
