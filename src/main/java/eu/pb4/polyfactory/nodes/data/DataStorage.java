package eu.pb4.polyfactory.nodes.data;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.*;
import com.kneelawk.graphlib.api.util.LinkPos;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.data.DataContainer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.pb4.polyfactory.ModInit.id;

public class DataStorage implements GraphEntity<DataStorage> {
    public static DataStorage EMPTY = new DataStorage() {
    };

    public static final GraphEntityType<DataStorage> TYPE = GraphEntityType.of(id("data_storage"), DataStorage::new, DataStorage::decode, DataStorage::split);

    private final Int2ObjectOpenHashMap<Set<BlockPos>> receivers = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Set<BlockPos>> providers = new Int2ObjectOpenHashMap<>();

    private final Int2ObjectOpenHashMap<DataContainer> currentData = new Int2ObjectOpenHashMap<>();
    private GraphEntityContext ctx;
    private long lastTick;

    public DataStorage() {
    }

    @Nullable
    public DataContainer getData(int channel) {
        if (!(this.ctx.getBlockWorld() instanceof ServerWorld world)) {
            return null;
        }

        var tick = world.getTime();
        if (this.lastTick != tick) {
            this.lastTick = tick;
            this.currentData.clear();
        }

        var current = this.currentData.get(channel);

        if (current != null) {
            return current;
        }

        var providers = this.providers.get(channel);
        if (providers == null) {
            return null;
        }

        for (var x : providers) {
            var state = world.getBlockState(x);
            if (state.getBlock() instanceof DataProvider provider) {
                current = provider.provideData(world, x, state, channel);
                if (current != null) {
                    this.currentData.put(channel, current);
                    return current;
                }
            }
        }
        return null;
    }

    public void pushDataUpdate(int channel, DataContainer data) {
        if (!(this.ctx.getBlockWorld() instanceof ServerWorld world)) {
            return;
        }

        var tick = world.getTime();
        if (this.lastTick != tick) {
            this.lastTick = tick;
            this.currentData.clear();
        }

        this.currentData.put(channel, data);


        var receivers = this.receivers.get(channel);
        if (receivers == null) {
            return;
        }

        for (var x : receivers) {
            var state = world.getBlockState(x);
            if (state.getBlock() instanceof DataReceiver receiver) {
                receiver.receiveData(world, x, state, channel, data);
            }
        }
    }

    @Override
    public void onNodeCreated(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity) {
        GraphEntity.super.onNodeCreated(node, nodeEntity);

        storeReceiverOrProvider(node);
        if (node.getBlockWorld() instanceof ServerWorld serverWorld) {
            if (node.getNode() instanceof DataProviderNode providerNode) {
                var state = node.getBlockWorld().getBlockState(node.getBlockPos());
                if (state.getBlock() instanceof DataProvider provider) {
                    var data = provider.provideData(serverWorld, node.getBlockPos(), state, providerNode.channel());
                    if (data != null) {
                        pushDataUpdate(providerNode.channel(), data);
                    }
                }
            } else if (node.getNode() instanceof DataReceiverNode receiverNode) {
                var state = node.getBlockWorld().getBlockState(node.getBlockPos());
                if (state.getBlock() instanceof DataReceiver receiver) {
                    var data = this.getData(receiverNode.channel());
                    if (data != null) {
                        receiver.receiveData(serverWorld, node.getBlockPos(), state, receiverNode.channel(), data);
                    }
                }
            }
        }
    }

    @Override
    public void onNodeDestroyed(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity, Map<LinkPos, LinkEntity> linkEntities) {
        GraphEntity.super.onNodeDestroyed(node, nodeEntity, linkEntities);
        if (node.getNode() instanceof DataReceiverNode data) {
            removeMap(this.receivers, data.channel(), node.getBlockPos());
        } else if (node.getNode() instanceof DataProviderNode data) {
            removeMap(this.providers, data.channel(), node.getBlockPos());
        }
    }

    private void storeReceiverOrProvider(NodeHolder<BlockNode> node) {
        if (node.getNode() instanceof DataReceiverNode data) {
            addMap(this.receivers, data.channel(), node.getBlockPos());
        } else if (node.getNode() instanceof DataProviderNode data) {
            addMap(this.providers, data.channel(), node.getBlockPos());
        }
    }

    private void addMap(Int2ObjectOpenHashMap<Set<BlockPos>> map, int channel, BlockPos blockPos) {
        var set = map.get(channel);
        if (set == null) {
            set = new HashSet<>();
            map.put(channel, set);
        }
        set.add(blockPos);
    }

    private void removeMap(Int2ObjectOpenHashMap<Set<BlockPos>> map, int channel, BlockPos blockPos) {
        var set = map.get(channel);
        if (set == null) {
            return;
        }
        set.remove(blockPos);
        if (set.isEmpty()) {
            map.remove(channel);
        }
    }

    @Override
    public void onInit(@NotNull GraphEntityContext ctx) {
        this.ctx = ctx;

        ctx.getGraph().getNodes().forEach(this::storeReceiverOrProvider);
    }

    @Override
    public @NotNull GraphEntityContext getContext() {
        return this.ctx;
    }

    @Override
    public @NotNull GraphEntityType<?> getType() {
        return TYPE;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return null;
    }

    @Override
    public void merge(@NotNull DataStorage other) {
        mergeMap(this.providers, other.providers);
        mergeMap(this.receivers, other.receivers);

        for (var channel : this.receivers.keySet()) {
            var data = getData(channel);
            if (data != null) {
                for (var x : this.receivers.get(channel)) {
                    var state = this.ctx.getBlockWorld().getBlockState(x);
                    if (state.getBlock() instanceof DataReceiver receiver) {
                        receiver.receiveData((ServerWorld) this.ctx.getBlockWorld(), x, state, channel, data);
                    }
                }
            }
        }
    }

    private static void mergeMap(Int2ObjectOpenHashMap<Set<BlockPos>> into, Int2ObjectOpenHashMap<Set<BlockPos>> from) {
        for (var entry : from.int2ObjectEntrySet()) {
            var self = into.get(entry.getIntKey());
            if (self == null) {
                into.put(entry.getIntKey(), new HashSet<>(entry.getValue()));
            } else {
                self.addAll(entry.getValue());
            }
        }
    }

    private static void splitMap(Int2ObjectOpenHashMap<Set<BlockPos>> into, Int2ObjectOpenHashMap<Set<BlockPos>> from, BlockGraph targetGraph) {
        for (var entry : from.int2ObjectEntrySet()) {
            var set = new HashSet<BlockPos>();
            for (var pos : List.copyOf(entry.getValue())) {
                if (targetGraph.getNodesAt(pos).findAny().isPresent()) {
                    set.add(pos);
                    entry.getValue().remove(pos);
                }
            }
            if (!set.isEmpty()) {
                into.put(entry.getIntKey(), set);
            }
        }
    }

    private @NotNull DataStorage split(@NotNull BlockGraph originalGraph, @NotNull BlockGraph newGraph) {
        var data = new DataStorage();
        splitMap(data.receivers, this.receivers, newGraph);
        splitMap(data.providers, this.providers, newGraph);
        return data;
    }

    private static DataStorage decode(@Nullable NbtElement nbtElement) {
        return new DataStorage();
    }
}
