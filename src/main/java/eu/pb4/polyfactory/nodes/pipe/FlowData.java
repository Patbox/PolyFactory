package eu.pb4.polyfactory.nodes.pipe;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.*;
import com.kneelawk.graphlib.api.util.LinkPos;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;

import static eu.pb4.polyfactory.ModInit.id;

public class FlowData implements GraphEntity<FlowData> {
    public static final Codec<FlowData> CODEC = Codec.unit(FlowData::new);
    public static final GraphEntityType<FlowData> TYPE = GraphEntityType.of(id("flow_data"), CODEC, FlowData::new, FlowData::split);
    public static FlowData EMPTY = new FlowData() {
        @Override
        public void runPushFlows(BlockPos pos, BooleanSupplier canContinue, FlowConsumer consumer) {}

        @Override
        public void setSourceStrength(BlockPos pos, double strength) {}
    };
    private GraphEntityContext ctx;

    private final Map<BlockPos, CurrentFlow> currentFlow = new HashMap<>();
    private final Object2DoubleMap<BlockPos> sourceStrength = new Object2DoubleOpenHashMap<>();
    private boolean isInvalid = true;

    public FlowData() {
    }

    public void runPushFlows(BlockPos pos, BooleanSupplier canContinue, FlowConsumer consumer) {
        runFlows(pos, true, canContinue, consumer);
    }

    public void runPullFlows(BlockPos pos, BooleanSupplier canContinue, FlowConsumer consumer) {
        runFlows(pos, false, canContinue, consumer);
    }
    public void runFlows(BlockPos pos, boolean push, BooleanSupplier canContinue, FlowConsumer consumer) {
        if (this.isInvalid) {
            this.rebuild();
        }
        var current = this.currentFlow.get(pos);
        if (current == null || !canContinue.getAsBoolean()) {
            return;
        }
        var flows = push ? current.push : current.pull;
        if (flows.isEmpty()) {
            return;
        }

        double total = 0;
        for (var flow : flows) {
            total += flow.strength * this.sourceStrength.getDouble(flow.source);
        }
        if (total == 0) {
            return;
        }
        for (var flow : flows) {
            if (!canContinue.getAsBoolean()) {
                return;
            }
            var s = this.sourceStrength.getDouble(flow.source);
            if (s > 0) {
                consumer.consume(flow.direction, s * flow.strength * s * flow.strength / 32 / total);
            }
        }
    }

    public void setSourceStrength(BlockPos pos, double strength) {
        if (strength == 0) {
            this.sourceStrength.removeDouble(pos);
        } else {
            this.sourceStrength.put(pos, strength);
        }
    }

    private void rebuild() {
        if (!this.isInvalid || this.ctx == null) {
            return;
        }
        this.isInvalid = false;
        var map = new HashMap<BlockPos, Pair<double[], double[]>>();

        for (var pump : this.ctx.getGraph().getCachedNodes(PumpNode.CACHE)) {
            var reverse = pump.getNode().reverse();
            Direction direction;
            double distance;
            var mut = new BlockPos.Mutable();
            var states = new ObjectArrayList<LastState>();
            states.add(new LastState(pump.getNode().direction(), pump.getBlockPos(), 32));
            do {
                var state = states.pop();
                direction = state.direction;
                distance = state.distance;
                mut.set(state.start);

                while (distance > 0.25) {
                    mut.move(direction);

                    var node = this.ctx.getGraph().getNodesAt(mut).findAny();
                    if (node.isEmpty() || node.get().getNode() instanceof PumpNode) {
                        break;
                    }
                    Pair<double[], double[]> flow;
                    if (map.containsKey(mut)) {
                        flow = map.get(mut);
                    } else {
                        flow = new Pair<>(new double[Direction.values().length], new double[Direction.values().length]);
                        map.put(mut.toImmutable(), flow);
                    }


                    var dirs = new ArrayList<Direction>();
                    for (var d : Direction.values()) {
                        if (d != direction.getOpposite() && node.get().getNode() instanceof DirectionCheckingNode check && check.canConnectDir(d)) {
                            dirs.add(d);
                        }
                    }
                    /*for (var conn : node.get().getConnections()) {
                        var other = conn.other(node.get());
                        var otherDir = Direction.fromVector(
                                other.getBlockPos().getX() - node.get().getBlockPos().getX(),
                                other.getBlockPos().getY() - node.get().getBlockPos().getY(),
                                other.getBlockPos().getZ() - node.get().getBlockPos().getZ()
                        );
                        if (otherDir != direction.getOpposite()) {
                            dirs.add(otherDir);
                        }
                    }*/

                    if (dirs.isEmpty()) {
                        break;
                    }

                    var remove = (reverse ? flow.getFirst() : flow.getSecond())[direction.getOpposite().ordinal()] == 0;

                    (reverse ? flow.getFirst() : flow.getSecond())[direction.getOpposite().ordinal()] += distance;
                    var multi = reverse ? flow.getSecond() : flow.getFirst();
                    for (var d : dirs) {
                        multi[d.ordinal()] += distance;
                    }

                    if (remove) {
                        distance--;
                    }
                    if (dirs.size() == 1) {
                        direction = dirs.get(0);
                    } else {
                        var cur = mut.toImmutable();
                        for (var d : dirs) {
                            states.push(new LastState(d, cur, distance / dirs.size()));
                        }
                        break;
                    }
                }
            } while (!states.isEmpty());
            map.forEach((pos, flow) -> {
                var curr = this.currentFlow.computeIfAbsent(pos, CurrentFlow::new);
                var push = flow.getFirst();
                var pull = flow.getSecond();
                for (var dir : Direction.values()) {
                    if (push[dir.ordinal()] > 0) {
                        curr.push.add(new DirectionalFlow(pump.getBlockPos(), dir, push[dir.ordinal()]));
                    }

                    if (pull[dir.ordinal()] > 0) {
                        curr.pull.add(new DirectionalFlow(pump.getBlockPos(), dir, pull[dir.ordinal()]));
                    }
                    push[dir.ordinal()] = 0;
                    pull[dir.ordinal()] = 0;
                }
            });
        }

    }

    private void invalidate() {
        if (this.isInvalid) {
            return;
        }
        this.currentFlow.clear();
        this.isInvalid = true;
    }

    @Override
    public void onPostNodeCreated(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity) {
        GraphEntity.super.onPostNodeCreated(node, nodeEntity);
        this.invalidate();
    }

    @Override
    public void onPostNodeDestroyed(@NotNull NodeHolder<BlockNode> node, @Nullable NodeEntity nodeEntity, Map<LinkPos, LinkEntity> linkEntities) {
        GraphEntity.super.onPostNodeDestroyed(node, nodeEntity, linkEntities);
        this.invalidate();
    }

    @Override
    public void merge(@NotNull FlowData flowData) {
        this.invalidate();
    }

    @Override
    public void onInit(@NotNull GraphEntityContext ctx) {
        this.ctx = ctx;
        this.rebuild();
    }

    @Override
    public @NotNull GraphEntityContext getContext() {
        return this.ctx;
    }

    @Override
    public @NotNull GraphEntityType<?> getType() {
        return TYPE;
    }

    private @NotNull FlowData split(@NotNull BlockGraph originalGraph, @NotNull BlockGraph newGraph) {
        this.invalidate();
        return new FlowData();
    }

    private record LastState(Direction direction, BlockPos start, double distance) {};

    private static class CurrentFlow {
        List<DirectionalFlow> push = new ArrayList<>();
        List<DirectionalFlow> pull = new ArrayList<>();

        public CurrentFlow(BlockPos pos) {
        }
    }

    private record DirectionalFlow(BlockPos source, Direction direction, double strength) {
    }

    public interface FlowConsumer {
        void consume(Direction direction, double strength);
    }
}
