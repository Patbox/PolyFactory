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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import static eu.pb4.polyfactory.ModInit.id;

public class DataStorage implements GraphEntity<DataStorage> {
    public static final int MAX_CHANNELS = 4;
    public static final Codec<DataStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DataEntry.CODEC.listOf().optionalFieldOf("current_data", List.of()).forGetter(e -> {
                var list = new ArrayList<DataEntry>();
                for (var x : e.currentData.int2ObjectEntrySet()) {
                    list.add(new DataEntry(x.getIntKey(), x.getValue().pos, x.getValue().container, Optional.ofNullable(x.getValue().direction), x.getValue().dataId));
                }
                return list;
            })
    ).apply(instance, DataStorage::new));
    public static final GraphEntityType<DataStorage> TYPE = GraphEntityType.of(id("data_storage"), CODEC, DataStorage::new, DataStorage::split);
    public static DataStorage EMPTY = new DataStorage() {
    };
    private final Int2ObjectOpenHashMap<Set<DataHandler<DataReceiverNode>>> receivers = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Set<DataHandler<DataProviderNode>>> providers = new Int2ObjectOpenHashMap<>();
    private Int2ObjectOpenHashMap<SentData> currentData = new Int2ObjectOpenHashMap<>();
    private Int2ObjectOpenHashMap<SentData> swapData = new Int2ObjectOpenHashMap<>();
    private GraphEntityContext ctx;
    public DataStorage() {
    }

    private DataStorage(List<DataEntry> entries) {
        for (var entry : entries) {
            this.currentData.put(entry.key, new SentData(entry.dataContainer, entry.blockPos, entry.direction.orElse(null), entry.dataId));
        }
    }

    @Deprecated
    @Nullable
    public SentData getData(int channel) {
        if (channel == -1 || this.ctx == null || !(this.ctx.getBlockWorld() instanceof ServerLevel world)) {
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
            var state = world.getBlockState(x.pos());
            if (state.getBlock() instanceof DataProvider provider) {
                var c = provider.provideData(world, x.pos(), state, channel, x.node());
                if (c != null) {
                    return new SentData(c, x.pos(), x.node() instanceof DirectionNode d ? d.direction() : null, 0);
                }
            }
        }
        return null;
    }

    public int pushDataUpdate(BlockPos pos, int channel, DataContainer data, @Nullable Direction direction, int dataId) {
        if (channel == -1) {
            return 0;
        }
        this.currentData.put(channel, new SentData(data, pos, direction, dataId));
        var receivers = this.receivers.get(channel);

        if (receivers == null) {
            return 0;
        }

        for (var receiver : receivers) {
            if (receiver.node.instant()) {
                var state = this.ctx.getBlockWorld().getBlockState(receiver.pos);
                if (state.getBlock() instanceof DataReceiver receiverx) {
                    receiverx.receiveData((ServerLevel) this.ctx.getBlockWorld(), receiver.pos, state, channel, data, receiver.node, pos, direction, dataId);
                }
            }
        }

        return receivers.size();
    }

    @Override
    public void onTick() {
        if (this.ctx == null || !(this.ctx.getBlockWorld() instanceof ServerLevel world)) {
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

            for (var data : current.int2ObjectEntrySet()) {
                var rec = receivers.get(data.getIntKey());
                if (rec != null) {
                    for (var x : rec) {
                        if (!x.node.instant()) {
                            var state = world.getBlockState(x.pos());
                            if (state.getBlock() instanceof DataReceiver receiver) {
                                receiver.receiveData(world, x.pos(), state, data.getIntKey(), data.getValue().container(), x.node(), data.getValue().pos, data.getValue().direction, data.getValue().dataId);
                            }
                        }
                    }
                }
            }
            current.clear();
            this.swapData = current;
        }
    }

    @Override
    public void onPostNodeCreated(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity) {
        GraphEntity.super.onPostNodeCreated(node, nodeEntity);
        storeReceiverOrProvider(node);
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

    private <T> void addMap(Int2ObjectOpenHashMap<Set<DataHandler<T>>> map, int channel, BlockPos blockPos, T data) {
        var set = map.get(channel);
        if (set == null) {
            set = new HashSet<>();
            map.put(channel, set);
        }
        set.add(new DataHandler<>(blockPos, data));
    }

    private <T> void removeMap(Int2ObjectOpenHashMap<Set<DataHandler<T>>> map, int channel, BlockPos blockPos, T data) {
        var set = map.get(channel);
        if (set == null) {
            return;
        }
        set.removeIf(x -> x.pos().equals(blockPos) && x.node().equals(data));
        if (set.isEmpty()) {
            map.remove(channel);
        }
    }

    @Override
    public void onInit(@NotNull GraphEntityContext ctx) {
        this.ctx = ctx;
        this.reinitializeCache();
    }

    private void reinitializeCache() {
        this.providers.clear();
        this.receivers.clear();

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
        this.reinitializeCache();

        for (var channel : this.receivers.keySet()) {
            var data = getData(channel);
            if (data != null) {
                for (var x : this.receivers.get(channel.intValue())) {
                    var state = this.ctx.getBlockWorld().getBlockState(x.pos());
                    if (state.getBlock() instanceof DataReceiver receiver) {
                        receiver.receiveData((ServerLevel) this.ctx.getBlockWorld(), x.pos(), state, channel, data.container, x.node(), data.pos, data.direction, data.dataId);
                    }
                }
            }
        }
    }

    private @NotNull DataStorage split(@NotNull BlockGraph originalGraph, @NotNull BlockGraph newGraph) {
        this.reinitializeCache();
        return new DataStorage();
    }

    public boolean hasReceivers() {
        return !this.receivers.isEmpty();
    }

    public boolean hasProviders() {
        return !this.providers.isEmpty();
    }

    @ApiStatus.Internal
    public final Int2ObjectOpenHashMap<Set<DataHandler<DataReceiverNode>>> receivers() {
        return this.receivers;
    }

    @ApiStatus.Internal
    public final Int2ObjectOpenHashMap<Set<DataHandler<DataProviderNode>>> providers() {
        return this.providers;
    }

    private record DataEntry(int key, BlockPos blockPos, DataContainer dataContainer, Optional<Direction> direction, int dataId) {
        private static final Codec<BlockPos> BLOCK_POS_CODEC = Codec.withAlternative(BlockPos.CODEC, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("X").forGetter(BlockPos::getX),
                Codec.INT.fieldOf("Y").forGetter(BlockPos::getY),
                Codec.INT.fieldOf("Z").forGetter(BlockPos::getZ)
        ).apply(instance, BlockPos::new)));
        public static final Codec<DataEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("__key").forGetter(DataEntry::key),
                BLOCK_POS_CODEC.fieldOf("__pos").forGetter(DataEntry::blockPos),
                DataContainer.MAP_CODEC.forGetter(DataEntry::dataContainer),
                Direction.CODEC.optionalFieldOf("__dir").forGetter(DataEntry::direction),
                Codec.INT.optionalFieldOf("__data_id", 0).forGetter(DataEntry::dataId)
        ).apply(instance, DataEntry::new));
    }

    public record SentData(DataContainer container, BlockPos pos, @Nullable Direction direction, int dataId) {
    }

    public record DataHandler<T>(BlockPos pos, T node) { }
}
