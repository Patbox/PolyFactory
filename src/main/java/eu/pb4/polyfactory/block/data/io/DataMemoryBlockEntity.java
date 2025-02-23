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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
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
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        if (nbt.contains("channel")) {
            this.channelInput = this.channelOutput = nbt.getInt("channel");
        } else {
            this.channelInput = nbt.getInt("input_channel");
            this.channelOutput = nbt.getInt("output_channel");
        }

        if (nbt.contains("data")) {
            this.data = DataContainer.fromNbt(nbt.getCompound("data"), lookup);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("input_channel", this.channelInput);
        nbt.putInt("output_channel", this.channelOutput);

        nbt.put("data", this.data.createNbt(lookup));}

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
        this.channelOutput  = components.getOrDefault(FactoryDataComponents.CHANNEL, this.outputChannel());
    }

    @Override
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        nbt.remove("channel");
        nbt.remove("data");
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
