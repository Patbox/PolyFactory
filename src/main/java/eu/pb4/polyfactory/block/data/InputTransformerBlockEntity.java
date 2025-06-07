package eu.pb4.polyfactory.block.data;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

public class InputTransformerBlockEntity extends LockableBlockEntity {
    protected DataContainer lastInput = DataContainer.empty();
    protected DataContainer lastOutput = DataContainer.empty();
    private int channelInput;
    private int channelOutput;

    public InputTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.INPUT_TRANSFORMER, pos, state);
    }

    protected InputTransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DataContainer lastInput() {
        return lastInput;
    }

    public DataContainer setLastInput(DataContainer lastInput1) {
        this.lastInput = lastInput1;
        this.markDirty();
        return lastInput1;
    }

    public DataContainer lastOutput() {
        return lastOutput;
    }

    public DataContainer setLastOutput(DataContainer lastOutput) {
        this.lastOutput = lastOutput;
        this.markDirty();
        return lastOutput;
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("input_channel", this.channelInput);
        view.putInt("output_channel", this.channelOutput);

        view.put("input_data", DataContainer.CODEC, this.lastInput);
        view.put("output_data", DataContainer.CODEC, this.lastOutput);
    }

    @Override
    public void readData(ReadView view) {
        super.readData(view);
        this.channelInput = view.getInt("input_channel", 0);
        this.channelOutput = view.getInt("output_channel", 0);

        this.lastInput = view.read("input_data", DataContainer.CODEC).orElse(DataContainer.empty());
        this.lastOutput = view.read("output_data", DataContainer.CODEC).orElse(DataContainer.empty());
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
}
