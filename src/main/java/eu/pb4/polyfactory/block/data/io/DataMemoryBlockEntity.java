package eu.pb4.polyfactory.block.data.io;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.util.DataCache;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class DataMemoryBlockEntity extends LockableBlockEntity implements DataCache {
    protected DataContainer data = DataContainer.empty();
    private int channelInput;
    private int channelOutput;

    public DataMemoryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.DATA_MEMORY, pos, state);
    }

    protected DataMemoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DataContainer data() {
        return data;
    }

    public DataContainer setData(DataContainer data) {
        this.data = data;
        this.markDirty();
        return data;
    }

    @Override
    public void readData(ReadView view) {
        super.readData(view);
        if (view.getInt("channel", -999) != -999) {
            this.channelInput = this.channelOutput = view.getInt("channel", 0);
        } else {
            this.channelInput = view.getInt("input_channel", 0);
            this.channelOutput = view.getInt("output_channel", 0);
        }

        this.data = view.read("data", DataContainer.CODEC).orElse(DataContainer.empty());
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("input_channel", this.channelInput);
        view.putInt("output_channel", this.channelOutput);

        view.put("data", DataContainer.CODEC, this.data);
    }

    public int inputChannel() {
        return this.channelInput;
    }


    public int outputChannel() {
        return this.channelOutput;
    }

    public void setInputChannel(int channel) {
        this.channelInput = channel;
        if (this.hasWorld()) {
            NetworkComponent.Data.updateDataAt(this.world, this.pos);
            this.markDirty();
        }
    }

    public void setOutputChannel(int channel) {
        this.channelOutput = channel;
        if (this.hasWorld()) {
            NetworkComponent.Data.updateDataAt(this.world, this.pos);
            this.markDirty();
        }
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        componentMapBuilder.add(FactoryDataComponents.STORED_DATA, this.data);
        componentMapBuilder.add(FactoryDataComponents.CHANNEL, this.outputChannel());
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        this.data = components.getOrDefault(FactoryDataComponents.STORED_DATA, this.data);
        this.channelOutput = components.getOrDefault(FactoryDataComponents.CHANNEL, this.outputChannel());
    }

    @Override
    public void removeFromCopiedStackData(WriteView view) {
        super.removeFromCopiedStackData(view);
        view.remove("channel");
        view.remove("data");
    }

    @Override
    public @Nullable DataContainer getCachedData() {
        return this.data;
    }

    @Override
    public void setCachedData(DataContainer lastData) {
        this.data = lastData;
        this.markDirty();
    }
}
