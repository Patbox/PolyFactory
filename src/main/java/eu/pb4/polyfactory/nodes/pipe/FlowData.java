package eu.pb4.polyfactory.nodes.pipe;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.*;
import com.kneelawk.graphlib.api.util.LinkPos;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.mixin.SimpleBlockGraphAccessor;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.BooleanSupplier;

import static eu.pb4.polyfactory.ModInit.id;

public class FlowData implements GraphEntity<FlowData> {
    public static final int RANGE = 32;
    public static final Direction[] DIRECTIONS = Direction.values();
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
                var x = ((RANGE - flow.strength) / RANGE);
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

    private void rebuild() {
        if (!this.isInvalid || this.ctx == null) {
            return;
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
            var mut = new BlockPos.Mutable();
            states.add(new LastState(pump.getNode().direction(), pump.getBlockPos(), RANGE));

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
                        map.put(mut.toImmutable(), flow);
                    }
                    if (flow.distance.getValue() >= distance) {
                        break;
                    } else if (flow.distance.getValue() < distance) {
                        flow.push.clear();
                        flow.pull.clear();
                    }

                    dirsI = 0;
                    for (int i = 0; i < DIRECTIONS.length; i++) {
                        var d = DIRECTIONS[i];
                        if (d != direction.getOpposite() && node.getNode() instanceof DirectionCheckingNode check && check.canConnectDir(d)) {
                            dirs[dirsI++] = d;
                        }
                    }

                    if (dirsI == 0) {
                        break;
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
                    if (dirsI == 1) {
                        direction = dirs[0];
                    } else {
                        var cur = mut.toImmutable();
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
                    curr.push.add(new DirectionalFlow(pump.getBlockPos(), dir, distancex));
                }
                for (var dir : pull) {
                    curr.pull.add(new DirectionalFlow(pump.getBlockPos(), dir, distancex));
                }

                push.clear();
                pull.clear();
                flow.distance.setValue(0);
            });
        }

        //if (ModInit.DEV_ENV) {
        //    ModInit.LOGGER.info("Rebuilding pipes took {}ms", System.currentTimeMillis() - time);
        //}
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

    private static class CurrentFlow {
        List<DirectionalFlow> push = new ArrayList<>();
        List<DirectionalFlow> pull = new ArrayList<>();

        public CurrentFlow(BlockPos pos) {
        }
    }

    private record DirectionalFlow(BlockPos source, Direction direction, double strength) {
    }

    private record CurrentState(MutableInt distance, EnumSet<Direction> pull, EnumSet<Direction> push, NodeHolder<BlockNode> node) {
    }

    public interface FlowConsumer {
        void consume(Direction direction, double strength);
    }
}
