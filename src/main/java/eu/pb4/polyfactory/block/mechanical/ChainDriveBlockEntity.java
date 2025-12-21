package eu.pb4.polyfactory.block.mechanical;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ChainDriveBlockEntity extends BlockEntity implements BlockEntityExtraListener {
    private final Map<BlockPos, Entry> connectedBlockPos = new HashMap<>();
    private final Map<BlockPos, ChainDriveBlock.Route> routes = new HashMap<>();
    @Nullable
    private ChainDriveBlock.Model model;

    public ChainDriveBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CHAIN_DRIVE, pos, state);
    }

    public boolean hasConnection(BlockPos pos) {
        return this.connectedBlockPos.containsKey(pos);
    }

    public ChainDriveBlock.Route getRoute(BlockPos pos) {
        return this.routes.get(pos);
    }

    public Collection<BlockPos> connections() {
        return this.connectedBlockPos.keySet();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null) {
            for (var connection : List.copyOf(this.connectedBlockPos.values())) {
                if (this.level.getBlockEntity(connection.pos) instanceof ChainDriveBlockEntity be) {
                    be.removeConnection(pos);
                    Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, connection.stack);
                }
            }
            this.connectedBlockPos.clear();
        }
    }

    public void removeConnection(BlockPos pos) {
        this.connectedBlockPos.remove(pos);
        this.routes.remove(pos);
        NetworkComponent.Rotational.updateRotationalAt(this.level, this.getBlockPos());
        this.setChanged();
        if (this.model != null) {
            this.model.removeConnection(pos);
        }
    }

    public boolean addConnection(BlockPos pos, Direction.Axis axis, ItemStack stack) {
        return addConnection(new Entry(pos, axis, stack));
    }

    private boolean addConnection(Entry entry) {
        if (this.connectedBlockPos.containsKey(entry.pos) || entry.pos.equals(this.getBlockPos())) {
            return false;
        }
        this.connectedBlockPos.put(entry.pos, entry);
        var route = this.updateRoute(entry);
        this.connectedBlockPos.put(entry.pos, entry);
        NetworkComponent.Rotational.updateRotationalAt(this.level, this.getBlockPos());
        this.setChanged();
        if (this.model != null) {
            this.model.addConnection(entry.pos, route);
        }
        return true;
    }

    private ChainDriveBlock.Route updateRoute(Entry entry) {
        var route = ChainDriveBlock.Route.create(this.getBlockState().getValue(ChainDriveBlock.AXIS), this.getBlockPos(), entry.axis, entry.pos);
        this.routes.put(entry.pos, route);
        return route;
    }

    public void updateAxis(BlockPos pos, Direction.Axis axis) {
        if (pos.equals(this.getBlockPos()) && this.model != null) {
            for (var val : List.copyOf(this.connectedBlockPos.values())) {
                if (ChainDriveBlock.getChainCost(pos, val.pos, axis, val.axis) < 0) {
                    this.removeConnection(val.pos);
                    if (this.level.getBlockEntity(val.pos) instanceof ChainDriveBlockEntity be) {
                        be.removeConnection(pos);
                        Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, val.stack);
                    }
                }
            }

            this.connectedBlockPos.values().forEach(this::updateRoute);
            this.model.setConnections(this.routes);
            return;
        }

        var entry = this.connectedBlockPos.get(pos);
        if (entry == null) {
            return;
        }
        entry = new Entry(pos, axis, entry.stack);
        this.connectedBlockPos.put(pos.immutable(), entry);
        var route = this.updateRoute(entry);
        if (this.model != null) {
            this.model.removeConnection(pos);
            this.model.addConnection(entry.pos, route);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.store("connection", Entry.COLLECTION_CODEC, this.connectedBlockPos.values());
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.connectedBlockPos.clear();
        this.routes.clear();
        view.read("connection", Entry.COLLECTION_CODEC).ifPresent(x -> x.forEach(this::addConnection));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard("connection");
    }

    @Override
    public void onListenerUpdate(LevelChunk worldChunk) {
        this.model = (ChainDriveBlock.Model) BlockAwareAttachment.get(worldChunk, worldPosition).holder();
        this.model.setConnections(this.routes);
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
