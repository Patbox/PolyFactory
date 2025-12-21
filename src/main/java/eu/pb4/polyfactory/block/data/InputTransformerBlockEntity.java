package eu.pb4.polyfactory.block.data;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
        this.setChanged();
        return lastInput1;
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
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("input_channel", this.channelInput);
        view.putInt("output_channel", this.channelOutput);

        view.store("input_data", DataContainer.CODEC, this.lastInput);
        view.store("output_data", DataContainer.CODEC, this.lastOutput);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.channelInput = view.getIntOr("input_channel", 0);
        this.channelOutput = view.getIntOr("output_channel", 0);

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
