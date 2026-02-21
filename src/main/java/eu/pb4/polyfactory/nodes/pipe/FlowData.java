package eu.pb4.polyfactory.nodes.pipe;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.*;
import com.kneelawk.graphlib.api.util.LinkPos;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.mixin.SimpleBlockGraphAccessor;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.DirectionNode;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.BooleanSupplier;

import static eu.pb4.polyfactory.ModInit.id;

public class FlowData implements GraphEntity<FlowData> {
    public static final Direction[][] DIRECTIONS = Util.make(new Direction[Direction.values().length][], arr -> {
        for (var dir : Direction.values()) {
            var preference = new Direction[Direction.values().length];
            preference[0] = dir;
            preference[1] = dir.getOpposite();
            var startAxis = switch (dir.getAxis()) {
                case Y, Z -> Direction.Axis.X;
                case X -> Direction.Axis.Z;
            };
            preference[2] = Direction.fromAxisAndDirection(startAxis, dir.getAxisDirection());
            preference[3] = preference[2].getOpposite();
            preference[4] = preference[2].getClockWise(dir.getAxis());
            preference[5] = preference[2].getCounterClockWise(dir.getAxis());

            arr[dir.ordinal()] = preference;
        }
    });
    public static final Codec<FlowData> CODEC = MapCodec.unit(FlowData::new).codec();
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

    public long[] getWeightedMaxFlow(BlockPos pos, boolean push, long maxFlow) {
        var vals = new long[6];

        if (this.isInvalid && !this.rebuild()) {
            return vals;
        }

        var current = this.currentFlow.get(pos);
        if (current == null) {
            return vals;
        }
        var flows = push ? current.push : current.pull;
        if (flows.isEmpty()) {
            return vals;
        }

        if (flows.size() == 1) {
            var flow = flows.getFirst();
            vals[flow.direction.ordinal()] = maxFlow;
            return vals;
        }

        double total = 0;
        var amount = new double[6];
        for (var flow : flows) {
            var t = flow.strength * this.sourceStrength.getDouble(flow.source);
            amount[flow.direction.ordinal()] += t;
            total += t;
        }

        var totalVals = 0l;
        var hasNonZeroVals = false;

        for (int i = 0; i < 6; i++) {
            vals[i] = (long) (maxFlow * amount[i] / total);
            totalVals += vals[i];
            hasNonZeroVals |= vals[i] != 0;
        }

        if (totalVals < maxFlow) {
            for (int i = 0; i < 6; i++) {
                if (hasNonZeroVals ? vals[i] != 0 : amount[i] != 0) {
                    vals[i] += maxFlow - totalVals;
                    break;
                }
            }
        }
        return vals;
    }

    public void runFlows(BlockPos pos, boolean push, BooleanSupplier canContinue, FlowConsumer consumer) {
        if (this.isInvalid && !this.rebuild()) {
            return;
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
                var x = ((flow.range - flow.strength) / flow.range);
                consumer.consume(flow.direction,  s * (1 - x * x * x) * (s * flow.strength / total));
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

    private boolean rebuild() {
        if (!this.isInvalid || this.ctx == null) {
            return false;
        }
        var time = System.currentTimeMillis();
        this.isInvalid = false;
        var map = new Object2ObjectOpenHashMap<BlockPos, CurrentState>(this.sourceStrength.size());
        var states = new ObjectArrayList<LastState>();
        var dirs = new Direction[6];
        var dirsI = 0;


        for (var pump : this.ctx.getGraph().getCachedNodes(PumpNode.CACHE)) {
            states.clear();
            var isPulling = pump.getNode().isPulling();
            Direction direction;
            int distance;
            var mut = new BlockPos.MutableBlockPos();
            states.add(new LastState(pump.getNode().direction(), pump.getBlockPos(), pump.getNode().range()));

            var nodeMap = ((SimpleBlockGraphAccessor) this.ctx.getGraph()).getNodesInPos();

            do {
                var state = states.pop();
                direction = state.direction;
                distance = state.distance;
                mut.set(state.start);

                while (distance > 0) {
                    mut.move(direction);
                    CurrentState flow = map.get(mut);
                    NodeHolder<BlockNode> node;
                    if (flow != null) {
                        node = flow.node;
                        if (node == null) {
                            break;
                        }
                    } else {
                        var iter = nodeMap.get(mut).iterator();
                        if (!iter.hasNext()) {
                            break;
                        }

                        node = iter.next();
                        if (node.getNode() instanceof PumpNode) {
                            break;
                        }

                        flow = new CurrentState(new MutableInt(), EnumSet.noneOf(Direction.class), EnumSet.noneOf(Direction.class), node);
                        map.put(mut.immutable(), flow);
                    }
                    if (flow.distance.getValue() >= distance) {
                        break;
                    } else if (flow.distance.getValue() < distance) {
                        flow.push.clear();
                        flow.pull.clear();
                    }

                    var directions = DIRECTIONS[direction.ordinal()];

                    dirsI = 0;
                    if (node.getNode() instanceof FlowNode check) {
                        for (int i = 0; i < directions.length; i++) {
                            var d = directions[i];
                            if (d != direction.getOpposite() && check.canFlowIn(d)) {
                                dirs[dirsI++] = d;
                            }
                        }
                    }

                    var main = (isPulling ? flow.push : flow.pull);
                    var multi = isPulling ? flow.pull : flow.push;
                    if (!main.add(direction.getOpposite())) {
                        break;
                    }

                    for (var i = 0; i < dirsI; i++) {
                        multi.add(dirs[i]);
                    }

                    flow.distance.setValue(distance--);

                    dirsI = 0;
                    if (node.getNode() instanceof DirectionCheckingNode check) {
                        for (int i = 0; i < directions.length; i++) {
                            var d = directions[i];
                            if (d != direction.getOpposite() && check.canConnectDir(d)) {
                                dirs[dirsI++] = d;
                            }
                        }
                    }


                    if (dirsI == 0) {
                        break;
                    }

                    if (dirsI == 1) {
                        direction = dirs[0];
                    } else {
                        var cur = mut.immutable();
                        boolean canContinue = false;
                        for (var i = 0; i < dirsI; i++) {
                            var d = dirs[i];
                            if (d == direction) {
                                canContinue = true;
                                continue;
                            }
                            states.push(new LastState(d, cur, distance));
                        }
                        if (!canContinue) {
                            break;
                        }
                    }
                }
            } while (!states.isEmpty());

            map.forEach((pos, flow) -> {
                var curr = this.currentFlow.computeIfAbsent(pos, CurrentFlow::new);
                var push = flow.push;
                var pull = flow.pull;
                var distancex = flow.distance.getValue();
                for (var dir : push) {
                    curr.push.add(new DirectionalFlow(pump.getBlockPos(), dir, distancex, pump.getNode().range()));
                }
                for (var dir : pull) {
                    curr.pull.add(new DirectionalFlow(pump.getBlockPos(), dir, distancex, pump.getNode().range()));
                }

                push.clear();
                pull.clear();
                flow.distance.setValue(0);
            });
        }

        return true;
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

    private record LastState(Direction direction, BlockPos start, int distance) {};

    public static class CurrentFlow {
        List<DirectionalFlow> push = new ArrayList<>();
        List<DirectionalFlow> pull = new ArrayList<>();

        public CurrentFlow(BlockPos pos) {
        }

        public List<DirectionalFlow> pull() {
            return pull;
        }

        public List<DirectionalFlow> push() {
            return push;
        }
    }

    public record DirectionalFlow(BlockPos source, Direction direction, double strength, int range) {
    }

    public record CurrentState(MutableInt distance, EnumSet<Direction> pull, EnumSet<Direction> push, NodeHolder<BlockNode> node) {
    }

    public interface FlowConsumer {
        void consume(Direction direction, double strength);
    }
}
