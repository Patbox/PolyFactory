package eu.pb4.polyfactory.block.data.io;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.util.DataCache;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
        this.setChanged();
        return data;
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        if (view.getIntOr("channel", -999) != -999) {
            this.channelInput = this.channelOutput = view.getIntOr("channel", 0);
        } else {
            this.channelInput = view.getIntOr("input_channel", 0);
            this.channelOutput = view.getIntOr("output_channel", 0);
        }

        this.data = view.read("data", DataContainer.CODEC).orElse(DataContainer.empty());
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("input_channel", this.channelInput);
        view.putInt("output_channel", this.channelOutput);

        view.store("data", DataContainer.CODEC, this.data);
    }

    public int inputChannel() {
        return this.channelInput;
    }


    public int outputChannel() {
        return this.channelOutput;
    }

    public void setInputChannel(int channel) {
        this.channelInput = channel;
        if (this.hasLevel()) {
            NetworkComponent.Data.updateDataAt(this.level, this.worldPosition);
            this.setChanged();
        }
    }

    public void setOutputChannel(int channel) {
        this.channelOutput = channel;
        if (this.hasLevel()) {
            NetworkComponent.Data.updateDataAt(this.level, this.worldPosition);
            this.setChanged();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
        componentMapBuilder.set(FactoryDataComponents.STORED_DATA, this.data);
        componentMapBuilder.set(FactoryDataComponents.CHANNEL, this.outputChannel());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.data = components.getOrDefault(FactoryDataComponents.STORED_DATA, this.data);
        this.channelOutput = components.getOrDefault(FactoryDataComponents.CHANNEL, this.outputChannel());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard("channel");
        view.discard("data");
    }

    @Override
    public @Nullable DataContainer getCachedData() {
        return this.data;
    }

    @Override
    public void setCachedData(DataContainer lastData) {
        this.data = lastData;
        this.setChanged();
    }
}
