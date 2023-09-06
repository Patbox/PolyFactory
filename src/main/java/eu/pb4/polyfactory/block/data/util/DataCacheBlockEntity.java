package eu.pb4.polyfactory.block.data.util;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.other.LockableBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class DataCacheBlockEntity extends LockableBlockEntity implements ChannelContainer {
    public DataContainer lastData = StringData.EMPTY;
    private int channel;

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.lastData = DataContainer.fromNbt(nbt.getCompound("data"));
        setChannel(nbt.getInt("channel"));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("channel", this.channel);
        nbt.put("data", this.lastData.createNbt());
    }

    public int channel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
        if (this.hasWorld()) {
            this.markDirty();
            NetworkComponent.Data.updateDataAt(this.world, this.pos);
        }
    }

    public DataCacheBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PROVIDER_DATA_CACHE, pos, state);
    }
}
