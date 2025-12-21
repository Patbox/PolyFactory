package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class ColorableBlockEntity extends BlockEntity implements BlockEntityExtraListener, ColorProvider {
    private int color = -2;
    @Nullable
    private ColorProvider.Consumer model;

    public ColorableBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.COLOR_CONTAINER, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("color", this.color);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        setColor(view.getIntOr("color", 0));
    }

    @Override
    public void setColor(int color) {
        if (this.color != color) {
            this.color = color;
            if (this.model != null) {
                this.model.setColor(color);
            }
            this.setChanged();
        }
    }

    @Override

    public int getColor() {
        return this.color;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = (Consumer) BlockBoundAttachment.get(chunk, this.getBlockPos()).holder();
        this.model.setColor(this.color);
    }

    @Override
    public boolean isDefaultColor() {
        return this.color == 0xFFFFFF;
    }
}
