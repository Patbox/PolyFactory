package eu.pb4.polyfactory.block.data;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class CableBlockEntity extends BlockEntity implements BlockEntityExtraListener, ColorProvider {
    private int color = -2;
    @Nullable
    private AbstractCableBlock.BaseCableModel model;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CABLE, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("color", this.color);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        setColor(view.getIntOr("color", this.color));
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
    public void setColorFromPreviousBlockEntity(int c) {
        setColor(c);
    }

    @Override
    public int getColor() {
        return this.color;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = (AbstractCableBlock.BaseCableModel) BlockBoundAttachment.get(chunk, this.getBlockPos()).holder();
        this.model.setColor(this.color);
    }

    @Override
    public boolean isDefaultColor() {
        return this.color == FactoryItems.CABLE.getDefaultColor();
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard("color");
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
        if (this.color != -2) {
            componentMapBuilder.set(FactoryDataComponents.COLOR, this.color);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        setColor(components.getOrDefault(FactoryDataComponents.COLOR, AbstractCableBlock.DEFAULT_COLOR));
    }
}
