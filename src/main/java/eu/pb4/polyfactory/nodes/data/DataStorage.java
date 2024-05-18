package eu.pb4.polyfactory.nodes.data;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.*;
import com.kneelawk.graphlib.api.util.LinkPos;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.DirectionNode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static eu.pb4.polyfactory.ModInit.id;

public class DataStorage implements GraphEntity<DataStorage> {
    public static final int MAX_CHANNELS = 4;
    public static final Codec<DataStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DataEntry.CODEC.listOf().optionalFieldOf("current_data", List.of()).forGetter(e -> {
                var list = new ArrayList<DataEntry>();
                for (var x : e.currentData.int2ObjectEntrySet()) {
                    list.add(new DataEntry(x.getIntKey(), x.getValue().pos, x.getValue().container, Optional.ofNullable(x.getValue().direction)));
                }
                return list;
            })
    ).apply(instance, DataStorage::new));
    public static final GraphEntityType<DataStorage> TYPE = GraphEntityType.of(id("data_storage"), CODEC, DataStorage::new, DataStorage::split);
    public static DataStorage EMPTY = new DataStorage() {
    };
    private final Int2ObjectOpenHashMap<Set<Pair<BlockPos, DataReceiverNode>>> receivers = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Set<Pair<BlockPos, DataProviderNode>>> providers = new Int2ObjectOpenHashMap<>();
    private Int2ObjectOpenHashMap<SentData> currentData = new Int2ObjectOpenHashMap<>();
    private Int2ObjectOpenHashMap<SentData> swapData = new Int2ObjectOpenHashMap<>();
    private GraphEntityContext ctx;
    public DataStorage() {
    }

    private DataStorage(List<DataEntry> entries) {
        for (var entry : entries) {
            this.currentData.put(entry.key, new SentData(entry.dataContainer, entry.blockPos, entry.direction.orElse(null)));
        }
    }

    private static <T> void mergeMap(Int2ObjectOpenHashMap<Set<T>> into, Int2ObjectOpenHashMap<Set<T>> from) {
        for (var entry : from.int2ObjectEntrySet()) {
            var self = into.get(entry.getIntKey());
            if (self == null) {
                into.put(entry.getIntKey(), new HashSet<>(entry.getValue()));
            } else {
                self.addAll(entry.getValue());
            }
        }
    }

    private static <T> void splitMap(Int2ObjectOpenHashMap<Set<Pair<BlockPos, T>>> into, Int2ObjectOpenHashMap<Set<Pair<BlockPos, T>>> from, BlockGraph targetGraph) {
        for (var entry : from.int2ObjectEntrySet()) {
            var set = new HashSet<Pair<BlockPos, T>>();
            for (var pos : List.copyOf(entry.getValue())) {
                if (targetGraph.getNodesAt(pos.getLeft()).findAny().isPresent()) {
                    set.add(pos);
                    entry.getValue().remove(pos);
                }
            }
            if (!set.isEmpty()) {
                into.put(entry.getIntKey(), set);
            }
        }
    }

    @Nullable
    public SentData getData(int channel) {
        if (this.ctx == null || !(this.ctx.getBlockWorld() instanceof ServerWorld world)) {
            return null;
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
            var state = world.getBlockState(x.getLeft());
            if (state.getBlock() instanceof DataProvider provider) {
                var c = provider.provideData(world, x.getLeft(), state, channel, x.getRight());
                if (c != null) {
                    return new SentData(c, x.getLeft(), x.getRight() instanceof DirectionNode d ? d.direction() : null);
                }
            }
        }
        return null;
    }

    public int pushDataUpdate(BlockPos pos, int channel, DataContainer data, @Nullable Direction direction) {
        this.currentData.put(channel, new SentData(data, pos, direction));
        var receivers = this.receivers.get(channel);
        if (receivers == null) {
            return 0;
        }

        return receivers.size();
    }

    @Override
    public void onTick() {
        if (this.ctx == null || !(this.ctx.getBlockWorld() instanceof ServerWorld world)) {
            return;
        }

        if (this.currentData.isEmpty()) {
            return;
        }

        if (this.receivers.isEmpty()) {
            this.currentData.clear();
        } else {
            var current = this.currentData;
            this.currentData = this.swapData;

            //var count = new Object2IntOpenHashMap<BlockPos>();

            for (var data : current.int2ObjectEntrySet()) {
                var rec = receivers.get(data.getIntKey());
                if (rec != null) {
                    for (var x : rec) {
                        //count.put(x.getLeft(), count.getInt(x.getLeft()) + 1);
                        var state = world.getBlockState(x.getLeft());
                        if (state.getBlock() instanceof DataReceiver receiver) {
                            receiver.receiveData(world, x.getLeft(), state, data.getIntKey(), data.getValue().container(), x.getRight(), data.getValue().pos, data.getValue().direction);
                        }
                    }
                }
            }

            //for (var c : count.object2IntEntrySet()) {
            //    DebugInfoSender.addGameTestMarker(world, c.getKey(), "                Count: " + c.getIntValue(), 0x88FFFF66, 60);
            //}

            current.clear();
            this.swapData = current;
        }
    }

    @Override
    public void onPostNodeCreated(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity) {
        GraphEntity.super.onPostNodeCreated(node, nodeEntity);

        storeReceiverOrProvider(node);
        if (node.getBlockWorld() instanceof ServerWorld serverWorld) {
            if (node.getNode() instanceof DataProviderNode providerNode) {
                var state = node.getBlockWorld().getBlockState(node.getBlockPos());
                if (state.getBlock() instanceof DataProvider provider) {
                    var data = provider.provideData(serverWorld, node.getBlockPos(), state, providerNode.channel(), providerNode);
                    if (data != null) {
                        pushDataUpdate(node.getBlockPos(), providerNode.channel(), data, providerNode instanceof DirectionNode d ? d.direction() : null);
                    }
                }
            } else if (node.getNode() instanceof DataReceiverNode receiverNode) {
                var state = node.getBlockWorld().getBlockState(node.getBlockPos());
                if (state.getBlock() instanceof DataReceiver receiver) {
                    var data = this.getData(receiverNode.channel());
                    if (data != null) {
                        receiver.receiveData(serverWorld, node.getBlockPos(), state, receiverNode.channel(), data.container(), receiverNode, data.pos(), data.direction);
                    }
                }
            }
        }
    }

    @Override
    public void onPostNodeDestroyed(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity, Map<LinkPos, LinkEntity> linkEntities) {
        GraphEntity.super.onPostNodeDestroyed(node, nodeEntity, linkEntities);
        if (node.getNode() instanceof DataReceiverNode data) {
            removeMap(this.receivers, data.channel(), node.getBlockPos(), data);
        } else if (node.getNode() instanceof DataProviderNode data) {
            removeMap(this.providers, data.channel(), node.getBlockPos(), data);
        }
    }

    private void storeReceiverOrProvider(NodeHolder<BlockNode> node) {
        if (node.getNode() instanceof DataReceiverNode data) {
            addMap(this.receivers, data.channel(), node.getBlockPos(), data);
        } else if (node.getNode() instanceof DataProviderNode data) {
            addMap(this.providers, data.channel(), node.getBlockPos(), data);
        }
    }

    private <T> void addMap(Int2ObjectOpenHashMap<Set<Pair<BlockPos, T>>> map, int channel, BlockPos blockPos, T data) {
        var set = map.get(channel);
        if (set == null) {
            set = new HashSet<>();
            map.put(channel, set);
        }
        set.add(new Pair<>(blockPos, data));
    }

    private <T> void removeMap(Int2ObjectOpenHashMap<Set<Pair<BlockPos, T>>> map, int channel, BlockPos blockPos, T data) {
        var set = map.get(channel);
        if (set == null) {
            return;
        }
        set.removeIf(x -> x.getLeft().equals(blockPos) && x.getRight().equals(data));
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
    public void merge(@NotNull DataStorage other) {
        mergeMap(this.providers, other.providers);
        mergeMap(this.receivers, other.receivers);

        for (var channel : this.receivers.keySet()) {
            var data = getData(channel);
            if (data != null) {
                for (var x : this.receivers.get(channel.intValue())) {
                    var state = this.ctx.getBlockWorld().getBlockState(x.getLeft());
                    if (state.getBlock() instanceof DataReceiver receiver) {
                        receiver.receiveData((ServerWorld) this.ctx.getBlockWorld(), x.getLeft(), state, channel, data.container, x.getRight(), data.pos, data.direction);
                    }
                }
            }
        }
    }

    private @NotNull DataStorage split(@NotNull BlockGraph originalGraph, @NotNull BlockGraph newGraph) {
        var data = new DataStorage();
        splitMap(data.receivers, this.receivers, newGraph);
        splitMap(data.providers, this.providers, newGraph);
        return data;
    }

    public boolean hasReceivers() {
        return !this.receivers.isEmpty();
    }

    public boolean hasProviders() {
        return !this.providers.isEmpty();
    }

    private record DataEntry(int key, BlockPos blockPos, DataContainer dataContainer, Optional<Direction> direction) {
        private static final Codec<BlockPos> BLOCK_POS_CODEC = Codec.withAlternative(BlockPos.CODEC, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("X").forGetter(BlockPos::getX),
                Codec.INT.fieldOf("Y").forGetter(BlockPos::getY),
                Codec.INT.fieldOf("Z").forGetter(BlockPos::getZ)
        ).apply(instance, BlockPos::new)));
        public static final Codec<DataEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("__key").forGetter(DataEntry::key),
                BLOCK_POS_CODEC.fieldOf("__pos").forGetter(DataEntry::blockPos),
                DataContainer.MAP_CODEC.forGetter(DataEntry::dataContainer),
                Direction.CODEC.optionalFieldOf("__dir").forGetter(DataEntry::direction)
        ).apply(instance, DataEntry::new));
    }

    public record SentData(DataContainer container, BlockPos pos, @Nullable Direction direction) {
    }
}
