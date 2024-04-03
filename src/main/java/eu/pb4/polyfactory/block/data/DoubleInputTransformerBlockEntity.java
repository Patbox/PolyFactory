package eu.pb4.polyfactory.block.data;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class DoubleInputTransformerBlockEntity extends LockableBlockEntity {
    protected DataContainer lastInput1 = StringData.EMPTY;
    protected DataContainer lastInput2 = StringData.EMPTY;
    protected DataContainer lastOutput = StringData.EMPTY;
    private int channelInput1;
    private int channelInput2;
    private int channelOutput;
    private boolean forceText = false;

    public DoubleInputTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.DOUBLE_INPUT_TRANSFORMER, pos, state);
    }

    protected DoubleInputTransformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean forceText() {
        return forceText;
    }

    public void setForceText(boolean forceText) {
        this.forceText = forceText;
        this.markDirty();
    }

    public DataContainer lastInput1() {
        return lastInput1;
    }

    public DataContainer setLastInput1(DataContainer lastInput1) {
        this.lastInput1 = lastInput1;
        this.markDirty();
        return lastInput1;
    }

    public DataContainer lastInput2() {
        return lastInput2;
    }

    public DataContainer setLastInput2(DataContainer lastInput2) {
        this.lastInput2 = lastInput2;
        this.markDirty();
        return lastInput2;
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
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.channelInput1 = nbt.getInt("input_channel_1");
        this.channelInput2 = nbt.getInt("input_channel_2");
        this.channelOutput = nbt.getInt("output_channel");

        this.lastInput1 = DataContainer.fromNbt(nbt.getCompound("input_data_1"));
        this.lastInput2 = DataContainer.fromNbt(nbt.getCompound("input_data_2"));
        this.lastOutput = DataContainer.fromNbt(nbt.getCompound("output_data"));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("input_channel_1", this.channelInput1);
        nbt.putInt("input_channel_2", this.channelInput2);
        nbt.putInt("output_channel", this.channelOutput);

        nbt.put("input_data_1", this.lastInput1.createNbt());
        nbt.put("input_data_2", this.lastInput2.createNbt());
        nbt.put("output_data", this.lastOutput.createNbt());
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
        if (this.hasWorld()) {
            NetworkComponent.Data.updateDataAt(this.world, this.pos);
            this.markDirty();
        }
    }

    public void setInputChannel2(int channel) {
        this.channelInput2 = channel;
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
