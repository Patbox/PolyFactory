package eu.pb4.polyfactory.block.mechanical;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ChainDriveBlockEntity extends BlockEntity implements BlockEntityExtraListener {
    private final Map<BlockPos, Entry> connectedBlockPos = new HashMap<>();
    @Nullable
    private ChainDriveBlock.Model model;

    public ChainDriveBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CHAIN_DRIVE, pos, state);
    }

    public Map<BlockPos, Entry> getConnection() {
        return this.connectedBlockPos;
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);
        if (this.world != null) {
            for (var connection : List.copyOf(this.connectedBlockPos.values())) {
                if (this.world.getBlockEntity(connection.pos) instanceof ChainDriveBlockEntity be) {
                    be.removeConnection(pos);
                    ItemScatterer.spawn(this.world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, connection.stack);
                }
            }
            this.connectedBlockPos.clear();
        }
    }

    public void removeConnection(BlockPos pos) {
        this.connectedBlockPos.remove(pos);
        NetworkComponent.Rotational.updateRotationalAt(this.world, this.getPos());
        this.markDirty();
        if (this.model != null) {
            this.model.removeConnection(pos);
        }
    }

    public boolean addConnection(BlockPos pos, Direction.Axis axis, ItemStack stack) {
        return addConnection(new Entry(pos, axis, stack));
    }

    private boolean addConnection(Entry entry) {
        if (this.connectedBlockPos.containsKey(entry.pos) || entry.pos.equals(this.getPos())) {
            return false;
        }
        this.connectedBlockPos.put(entry.pos, entry);
        NetworkComponent.Rotational.updateRotationalAt(this.world, this.getPos());
        this.markDirty();
        if (this.model != null) {
            var selfAxis = this.getCachedState().get(ChainDriveBlock.AXIS);
            this.model.addConnection(selfAxis, entry.pos, entry.axis);
        }
        return true;
    }

    public void updateAxis(BlockPos pos, Direction.Axis axis) {
        if (pos.equals(this.getPos()) && this.model != null) {
            this.model.setConnections(this.connectedBlockPos);
            return;
        }

        var entry = this.connectedBlockPos.get(pos);
        if (entry == null) {
            return;
        }
        this.connectedBlockPos.put(pos.toImmutable(), new Entry(pos, axis, entry.stack));
        if (this.model != null) {
            var selfAxis = this.getCachedState().get(ChainDriveBlock.AXIS);
            this.model.removeConnection(pos);
            this.model.addConnection(selfAxis, pos.toImmutable(), axis);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.put("connection", Entry.COLLECTION_CODEC, this.connectedBlockPos.values());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.connectedBlockPos.clear();
        view.read("connection", Entry.COLLECTION_CODEC).ifPresent(x -> x.forEach(this::addConnection));
    }

    @Override
    public void removeFromCopiedStackData(WriteView view) {
        super.removeFromCopiedStackData(view);
        view.remove("connection");
    }

    @Override
    public void onListenerUpdate(WorldChunk worldChunk) {
        this.model = (ChainDriveBlock.Model) BlockAwareAttachment.get(worldChunk, pos).holder();
        this.model.setConnections(this.connectedBlockPos);
    }

    public record Entry(BlockPos pos, Direction.Axis axis, ItemStack stack) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                Direction.Axis.CODEC.fieldOf("axis").forGetter(Entry::axis),
                ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(Entry::stack)
        ).apply(instance, Entry::new));

        public static final Codec<Collection<Entry>> COLLECTION_CODEC = CODEC.listOf().xmap(Function.identity(), List::copyOf);
    }
}
