package eu.pb4.polyfactory.block.data.util;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlockEntity;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class ChanneledDataBlockEntity extends LockableBlockEntity implements ChanneledDataCache, BlockEntityExtraListener, ColorProvider {
    private int color = -2;
    @Nullable
    private AbstractCableBlock.BaseCableModel model;
    protected DataContainer lastData = DataContainer.empty();
    private int channel;

    @Nullable
    public DataContainer getCachedData() {
        return this.lastData;
    }

    public void setCachedData(DataContainer lastData) {
        this.lastData = lastData;
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.lastData = view.read("data", DataContainer.CODEC).orElse(DataContainer.empty());
        setChannel(view.getIntOr("channel", 0));
        this.color = view.getIntOr("color", AbstractCableBlock.DEFAULT_COLOR);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("channel", this.channel);
        view.store("data", DataContainer.CODEC, this.lastData);
        view.putInt("color", this.color);
    }

    public int channel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
        if (this.hasLevel()) {
            NetworkComponent.Data.updateDataAt(this.level, this.worldPosition);
            this.setChanged();
        }
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
    public void setColorFromPreviousBlockEntity(int c) {
        setColor(c);
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        var x = BlockBoundAttachment.get(chunk, this.getBlockPos());
        if (x != null && x.holder() instanceof AbstractCableBlock.BaseCableModel baseCableModel) {
            this.model = baseCableModel;
            baseCableModel.setColor(this.color);
        }

        if (x != null && x.holder() instanceof InitDataListener initDataListener && this.lastData != null) {
            initDataListener.provideInitialCachedData(this.lastData);
        }
    }

    @Override
    public boolean isDefaultColor() {
        return this.color == FactoryItems.CABLE.getDefaultColor();
    }


    public ChanneledDataBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PROVIDER_DATA_CACHE, pos, state);
    }

    public static BlockEntity migrating(BlockPos pos, BlockState state) {
        if (state.is(FactoryBlocks.DATA_MEMORY)) {
            return new DataMemoryBlockEntity(pos, state);
        }
        return new ChanneledDataBlockEntity(pos, state);
    }

    protected ChanneledDataBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
        componentMapBuilder.set(FactoryDataComponents.STORED_DATA, this.lastData);
        componentMapBuilder.set(FactoryDataComponents.CHANNEL, this.channel);
        if (this.color != -2) {
            componentMapBuilder.set(FactoryDataComponents.COLOR, this.color);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.lastData = components.getOrDefault(FactoryDataComponents.STORED_DATA, this.lastData);
        this.channel = components.getOrDefault(FactoryDataComponents.CHANNEL, this.channel);
        setColor(components.getOrDefault(FactoryDataComponents.COLOR, this.color));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard("channel");
        view.discard("data");
        view.discard("color");
    }


    public interface InitDataListener {
        void provideInitialCachedData(DataContainer lastData);
    }
}
