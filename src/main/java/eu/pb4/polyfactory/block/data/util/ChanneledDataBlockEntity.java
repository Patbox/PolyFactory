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
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
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
    public void readData(ReadView view) {
        super.readData(view);
        this.lastData = view.read("data", DataContainer.CODEC).orElse(DataContainer.empty());
        setChannel(view.getInt("channel", 0));
        this.color = view.getInt("color", AbstractCableBlock.DEFAULT_COLOR);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("channel", this.channel);
        view.put("data", DataContainer.CODEC, this.lastData);
        view.putInt("color", this.color);
    }

    public int channel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
        if (this.hasWorld()) {
            NetworkComponent.Data.updateDataAt(this.world, this.pos);
            this.markDirty();
        }
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
    public void setColorFromPreviousBlockEntity(int c) {
        setColor(c);
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        var x = BlockBoundAttachment.get(chunk, this.getPos());
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
        if (state.isOf(FactoryBlocks.DATA_MEMORY)) {
            return new DataMemoryBlockEntity(pos, state);
        }
        return new ChanneledDataBlockEntity(pos, state);
    }

    protected ChanneledDataBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        componentMapBuilder.add(FactoryDataComponents.STORED_DATA, this.lastData);
        componentMapBuilder.add(FactoryDataComponents.CHANNEL, this.channel);
        if (this.color != -2) {
            componentMapBuilder.add(FactoryDataComponents.COLOR, this.color);
        }
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        this.lastData = components.getOrDefault(FactoryDataComponents.STORED_DATA, this.lastData);
        this.channel = components.getOrDefault(FactoryDataComponents.CHANNEL, this.channel);
        setColor(components.getOrDefault(FactoryDataComponents.COLOR, this.color));
    }

    @Override
    public void removeFromCopiedStackData(WriteView view) {
        super.removeFromCopiedStackData(view);
        view.remove("channel");
        view.remove("data");
        view.remove("color");
    }


    public interface InitDataListener {
        void provideInitialCachedData(DataContainer lastData);
    }
}
