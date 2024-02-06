package eu.pb4.polyfactory.block.data.util;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class ChanneledDataBlockEntity extends LockableBlockEntity implements ChanneledDataCache, BlockEntityExtraListener {
    protected DataContainer lastData = StringData.EMPTY;
    private int channel;
    private ModelInitializer updateCaller;

    @Nullable
    public DataContainer getCachedData() {
        return this.lastData;
    }

    public void setCachedData(DataContainer lastData) {
        this.lastData = lastData;
        if (this.updateCaller != null) {
            this.updateCaller.setData(lastData);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("data")) {
            this.lastData = DataContainer.fromNbt(nbt.getCompound("data"));
        }
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

    public ChanneledDataBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PROVIDER_DATA_CACHE, pos, state);
    }

    protected ChanneledDataBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        if (this.lastData != null) {
            var x = BlockAwareAttachment.get(chunk, this.pos);
            if (x != null && x.holder() instanceof ModelInitializer call) {
                call.setData(this.lastData);
                this.updateCaller = call;
            }
        }
    }
    
    public interface ModelInitializer {
        void setData(DataContainer data);
    }
}
