package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.other.LockableBlockEntity;
import eu.pb4.polyfactory.data.FactoryData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class ProviderDataCacheBlockEntity extends LockableBlockEntity {
    public FactoryData lastData;
    private int channel;

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        setChannel(nbt.getInt("Channel"));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("Channel", this.channel);
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

    public ProviderDataCacheBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PROVIDER_DATA_CACHE, pos, state);
    }
}
