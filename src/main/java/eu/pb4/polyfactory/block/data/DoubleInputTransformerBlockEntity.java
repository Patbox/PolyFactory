package eu.pb4.polyfactory.block.data;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DoubleInputTransformerBlockEntity extends LockableBlockEntity {
    protected DataContainer lastInput1 = DataContainer.empty();
    protected DataContainer lastInput2 = DataContainer.empty();
    protected DataContainer lastOutput = DataContainer.empty();
    private int channelInput1;
    private int channelInput2;
    private int channelOutput;

    public DoubleInputTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.DOUBLE_INPUT_TRANSFORMER, pos, state);
    }

    protected DoubleInputTransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setForceText(boolean forceText) {
        this.setChanged();
    }

    public DataContainer lastInput1() {
        return lastInput1;
    }

    public DataContainer setLastInput1(DataContainer lastInput1) {
        this.lastInput1 = lastInput1;
        this.setChanged();
        return lastInput1;
    }

    public DataContainer lastInput2() {
        return lastInput2;
    }

    public DataContainer setLastInput2(DataContainer lastInput2) {
        this.lastInput2 = lastInput2;
        this.setChanged();
        return lastInput2;
    }

    public DataContainer lastOutput() {
        return lastOutput;
    }

    public DataContainer setLastOutput(DataContainer lastOutput) {
        this.lastOutput = lastOutput;
        this.setChanged();
        return lastOutput;
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.channelInput1 = view.getIntOr("input_channel_1", 0);
        this.channelInput2 = view.getIntOr("input_channel_2", 0);
        this.channelOutput = view.getIntOr("output_channel", 0);

        this.lastInput1 = view.read("input_data_1", DataContainer.CODEC).orElse(DataContainer.empty());
        this.lastInput2 = view.read("input_data_2", DataContainer.CODEC).orElse(DataContainer.empty());
        this.lastOutput = view.read("output_data", DataContainer.CODEC).orElse(DataContainer.empty());
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("input_channel_1", this.channelInput1);
        view.putInt("input_channel_2", this.channelInput2);
        view.putInt("output_channel", this.channelOutput);

        view.store("input_data_1", DataContainer.CODEC, this.lastInput1);
        view.store("input_data_2", DataContainer.CODEC, this.lastInput2);
        view.store("output_data", DataContainer.CODEC, this.lastOutput);
    }

    public int inputChannel1() {
        return this.channelInput1;
    }

    public int inputChannel2() {
        return this.channelInput2;
    }

    public int outputChannel() {
        return this.channelOutput;
    }

    public void setInputChannel1(int channel) {
        this.channelInput1 = channel;
        if (this.hasLevel()) {
            NetworkComponent.Data.updateDataAt(this.level, this.worldPosition);
            this.setChanged();
        }
    }

    public void setInputChannel2(int channel) {
        this.channelInput2 = channel;
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
}
