package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.GraphView;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.GraphEntity;
import com.kneelawk.graphlib.api.graph.user.GraphEntityType;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import eu.pb4.polyfactory.nodes.mechanical_connectors.LargeGearNode;
import eu.pb4.polyfactory.nodes.mechanical_connectors.SmallGearNode;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static eu.pb4.polyfactory.ModInit.id;

public class RotationData implements GraphEntity<RotationData> {
    public static RotationData EMPTY = new RotationData() {
        @Override
        public void update(ServerWorld world) {}
    };

    public static final GraphEntityType<RotationData> TYPE = GraphEntityType.of(id("rotation_info"), RotationData::new, RotationData::decode, RotationData::split);

    private double speed;
    private float rotation;
    private int lastTick = -1;
    private double stressCapacity;
    private double stressUsage;
    private GraphEntityContext ctx;
    private boolean negative;
    public RotationData() {
    }

    public double speed() {
        return this.speed;
    }

    public float rotation() {
        return this.rotation;
    }

    public double stressCapacity() {
        return stressCapacity;
    }

    public double stressUsage() {
        return this.stressUsage;
    }
    public boolean isNegative() {
        return this.negative;
    }

    public void update(ServerWorld world) {
        var currentTick = world.getServer().getTicks();
        if (this.lastTick == currentTick) {
            return;
        }
        var delta = this.lastTick == -1 ? 1 : currentTick - this.lastTick;
        this.lastTick = currentTick;

        var connectors = this.ctx.getGraph().getCachedNodes(AxleWithGearMechanicalNode.CACHE);
        if (connectors.isEmpty()) {
            this.updateSelf(world, delta);
        } else {
            var dirMap = new Long2BooleanOpenHashMap();
            var speedMap = new Long2FloatOpenHashMap();
            var clogged = new MutableBoolean();
            var rotationDataList = new ArrayList<RotationData>();

            var checkedNodes = new ObjectOpenHashSet<BlockPos>();
            collectGraphs(connectors, checkedNodes, dirMap, speedMap, clogged,  rotationDataList, false, 1);
            /*{
                var first = connectors.stream().findFirst().get();

                DebugInfoSender.addGameTestMarker((ServerWorld) first.getBlockWorld(), first.getBlockPos(),
                        "Start: " + first.getBlockWorld().getServer().getTicks() + " Size: " + rotationDataList.size(), (int) (0x8844FF44 + (first.getBlockWorld().getTime() % 16)), 50 * 4 * 20);
            }*/

            if (rotationDataList.isEmpty()) {
                updateSelf(world, delta);
                return;
            }
            for (var data : rotationDataList) {
                data.lastTick = currentTick;
            }

            if (clogged.booleanValue()) {
                for (var rot : rotationDataList) {
                    rot.lastTick = currentTick;
                    rot.speed = 0;
                    rot.stressUsage = 0;
                    rot.stressCapacity = 0;
                    rot.getContext().getGraph().getNodes().forEach(this::spawnSmoke);
                }
                return;
            }

            var usage = new Long2DoubleOpenHashMap();

            var state = new State();
            for (var data : rotationDataList) {
                state.flip = dirMap.get(data.getContext().getGraph().getId());
                state.multiplier = speedMap.get(data.getContext().getGraph().getId());
                data.calculateState(world, state);
                usage.put(data.getContext().getGraph().getId(), state.stressUsed);
                state.stressUsed = 0;
            }
            double fullStress = 0;

            boolean overloaded = false;

            for (var data : rotationDataList) {
                state.multiplier = speedMap.get(data.getContext().getGraph().getId());
                var stress = usage.get(data.getContext().getGraph().getId());
                if (Math.abs(state.finalStressCapacity()) - stress < 0) {
                    overloaded = true;
                }
                fullStress += stress / state.multiplier;
            }
            state.multiplier = 1;
            if (overloaded || Math.abs(state.stressCapacity) - fullStress < 0) {
                for (var rot : rotationDataList) {
                    rot.lastTick = currentTick;
                    rot.speed = 0;
                    rot.stressUsage = 0;
                    rot.stressCapacity = 0;
                    if (state.providerCount != 0) {
                        rot.getContext().getGraph().getNodes().forEach(this::spawnSmoke);
                    }
                }
                return;
            }
            state.stressUsed = fullStress;

            var negative = state.speed < 0;
            float biggest = 1;
            for (var data : rotationDataList) {
                data.negative = dirMap.get(data.getContext().getGraph().getId()) != negative;
                state.multiplier = speedMap.get(data.getContext().getGraph().getId());
                biggest = Math.max(biggest, state.multiplier);
                data.applyState(state);
            }

            if (this.speed == 0) {
                return;
            }

            var speed = this.speed;
            if (this.negative) {
                speed = -speed;
            }

            float r = (float) ((Math.min(speed * MathHelper.RADIANS_PER_DEGREE * delta, RotationConstants.VISUAL_UPDATE_RATE * delta) + this.rotation) % (MathHelper.TAU * biggest));
            if (this.negative) {
                r = -r;
            }
            r = Math.abs(r);

            for (var data : rotationDataList) {
                data.rotation = (data.negative ? -r : r) / speedMap.get(data.getContext().getGraph().getId());
            }
        }
    }

    private void spawnSmoke(NodeHolder<BlockNode> entry) {
        if (entry.getNode() instanceof FunctionalNode) {
            var world = (ServerWorld) this.ctx.getBlockWorld();
            var pos = entry.getBlockPos();
            world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.2);
        }
    }

    private static void collectGraphs(Collection<NodeHolder<AxleWithGearMechanicalNode>> connectors, ObjectOpenHashSet<BlockPos> checkedNodes,
                                      Long2BooleanOpenHashMap dirMap, Long2FloatOpenHashMap speedMap, MutableBoolean clogged, ArrayList<RotationData> data,
                                      boolean currentDirection, float speed) {
        for (var connection : connectors) {
            //DebugInfoSender.addGameTestMarker((ServerWorld) connection.getBlockWorld(), connection.getBlockPos(), "Alt", 0x88FFFF66, 50 * 4 * 20);

            var connectGraph = Objects.requireNonNull(FactoryNodes.ROTATIONAL_CONNECTOR.getSidedGraphView(connection.getBlockWorld()))
                    .getAllGraphsAt(connection.getBlockPos()).findFirst();
            if (connectGraph.isPresent() /*&& checked2.add(connectGraph.get().getId())*/) {
                var self = connectGraph.get().getNodesAt(connection.getBlockPos()).findFirst();
                if (self.isPresent()) {
                    iterateConnectedNodes(self.get(), connection.getGraphWorld(), checkedNodes, dirMap, speedMap, clogged, data, currentDirection, speed);
                }
            }
        }
    }

    private static void iterateConnectedNodes(NodeHolder<BlockNode> blockNodeNodeHolder, GraphView graphWorld, ObjectOpenHashSet<BlockPos> checkedSelf,
                                              Long2BooleanOpenHashMap dirMap, Long2FloatOpenHashMap speedMap, MutableBoolean clogged, ArrayList<RotationData> data,
                                              boolean currentDirection, float speed) {
        if (!checkedSelf.add(blockNodeNodeHolder.getBlockPos())) {
            //DebugInfoSender.addGameTestMarker((ServerWorld) graphWorld.getWorld(), blockNodeNodeHolder.getBlockPos(), "Dupe: " + blockNodeNodeHolder.getBlockPos().toShortString(), 0x88FF8888, 100);
            return;
        }

        for (var conn : blockNodeNodeHolder.getConnections()) {
            var x = conn.other(blockNodeNodeHolder);
            //DebugInfoSender.addGameTestMarker((ServerWorld) graphWorld.getWorld(), x.getBlockPos(), "Conn: " + blockNodeNodeHolder.getBlockPos().toShortString(), 0x88FFFFFF, 50 * 4 * 20);

            var nextGearSpeed = speed;
            boolean nextDirection;
            if (blockNodeNodeHolder.getNode().getClass() != x.getNode().getClass()) {
                if (blockNodeNodeHolder.getNode() instanceof SmallGearNode) {
                    nextGearSpeed *= 2;
                } else {
                    nextGearSpeed /= 2;
                }
                nextDirection = !currentDirection;
            } else if (blockNodeNodeHolder.getNode() instanceof LargeGearNode a) {
                var delta = blockNodeNodeHolder.getBlockPos().subtract(x.getBlockPos());

                nextDirection = (delta.getComponentAlongAxis(a.axis()) == delta.getComponentAlongAxis(((LargeGearNode) x.getNode()).axis())) == currentDirection;
            } else {
                nextDirection = !currentDirection;
            }

            iterateConnectedNodes(x, graphWorld, checkedSelf, dirMap, speedMap, clogged, data, nextDirection, nextGearSpeed);

            var optionalGraph = graphWorld.getAllGraphsAt(x.getBlockPos()).findFirst();
            if (optionalGraph.isEmpty()) {
                return;
            }

            var targetGraph = optionalGraph.get();

            if (dirMap.containsKey(targetGraph.getId())) {
                if (dirMap.get(targetGraph.getId()) == !nextDirection
                        || !MathHelper.approximatelyEquals(speedMap.get(targetGraph.getId()), nextGearSpeed)) {
                    clogged.setTrue();
                }
            } else {
                dirMap.put(targetGraph.getId(), nextDirection);
                speedMap.put(targetGraph.getId(), nextGearSpeed);
                data.add(targetGraph.getGraphEntity(TYPE));
            }

            var subConnectors = targetGraph.getCachedNodes(AxleWithGearMechanicalNode.CACHE);
            if (!subConnectors.isEmpty()) {
                collectGraphs(subConnectors, checkedSelf, dirMap, speedMap, clogged, data, nextDirection, nextGearSpeed);
            }
        }
    }

    private void calculateState(ServerWorld world, State state) {
        var list = this.ctx.getGraph().getCachedNodes(FunctionalDirectionNode.CACHE);

        if (list.isEmpty()) {
            return;
        }

        for (var entries : list) {
            var blockState = world.getBlockState(entries.getBlockPos());

            if (entries.getNode().getTargetFunctional(world, entries.getBlockPos(), blockState) instanceof RotationUser rotationalSource) {
                rotationalSource.updateRotationalData(state, blockState, world, entries.getBlockPos());
            }
        }
    }

    private void applyState(State state) {
        if (state.providerCount == 0) {
            this.stressCapacity = 0;
            this.stressUsage = 0;
            this.speed = 0;
            return;
        }
        this.stressCapacity = Math.abs(state.stressCapacity);
        this.stressUsage = state.stressUsed;

        if (this.stressCapacity - stressUsage < 0) {
            this.speed = 0;
        } else {
            this.speed = Math.abs(state.finalSpeed() / state.providerCount);
        }
    }

    private void updateSelf(ServerWorld world, float delta) {
        var graph = this.ctx.getGraph();
        var state = new State();
        calculateState(world, state);
        applyState(state);

        this.negative = state.speed < 0;

        var speed = this.speed != 0 ? this.speed / Math.signum(state.speed) : 0;

        if (Math.abs(state.stressCapacity) - stressUsage < 0) {
            for (var entry : graph.getCachedNodes(FunctionalDirectionNode.CACHE)) {
                var pos = entry.getBlockPos();
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.2);
            }
        }

        this.rotation = (float) ((Math.min(speed * MathHelper.RADIANS_PER_DEGREE * delta, RotationConstants.VISUAL_MAX_ROTATION * delta) + this.rotation) % MathHelper.TAU);
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

    @Override
    public @Nullable NbtElement toTag() {
        var nbt = new NbtCompound();
        nbt.putFloat("Rot", this.rotation);
        return nbt;
    }

    @Override
    public void onUnload() {

    }

    @Override
    public void merge(@NotNull RotationData other) {
        if (this.speed < other.speed) {
            this.rotation = other.rotation;
        }
    }

    private @NotNull RotationData split(@NotNull BlockGraph originalGraph, @NotNull BlockGraph newGraph) {
        var data = new RotationData();
        data.rotation = this.rotation;
        data.speed = this.speed;
        return data;
    }

    private static RotationData decode(@Nullable NbtElement nbtElement) {
        var rotation = new RotationData();
        if (nbtElement instanceof NbtCompound compound) {
            rotation.rotation = compound.getFloat("Rot");
        }

        return rotation;
    }

    public static class State {
        public static final State EMPTY = new State();
        public static final State SPECIAL = new State();
        public boolean flip = false;
        public float multiplier = 1;
        protected double speed;
        protected double stressCapacity;
        protected double stressUsed;
        protected int providerCount;
        protected int userCount;

        public void provide(double speed, double stressCapacity, boolean negative) {
            this.providerCount++;

            if (this.flip == negative) {
                this.speed += speed * this.multiplier;
                this.stressCapacity += stressCapacity / this.multiplier;
            } else {
                this.speed -= speed * this.multiplier;
                this.stressCapacity -= stressCapacity / this.multiplier;
            }
        }

        public void stress(double stress) {
            this.stressUsed += stress;
            this.userCount++;
        }

        public void provide(float speed, float stressCapacity, Direction.AxisDirection direction) {
            provide(speed, stressCapacity, direction == Direction.AxisDirection.NEGATIVE);
        }

        public double finalStressCapacity() {
            return this.stressCapacity * this.multiplier;
        }

        public double finalStressUsed() {
            return this.stressUsed;
        }

        public double finalSpeed() {
            return this.speed / this.multiplier;
        }

        public void clear() {
            this.speed = 0;
            this.userCount = 0;
            this.flip = false;
            this.multiplier = 1;
            this.stressCapacity = 0;
            this.stressUsed = 0;
            this.providerCount = 0;

        }
    }
}
