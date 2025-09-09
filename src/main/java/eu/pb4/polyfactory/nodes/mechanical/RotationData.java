package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.GraphView;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.GraphEntity;
import com.kneelawk.graphlib.api.graph.user.GraphEntityType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import eu.pb4.polyfactory.nodes.mechanical_connectors.LargeGearNode;
import eu.pb4.polyfactory.nodes.mechanical_connectors.SmallGearNode;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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
    public static final Codec<RotationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.optionalFieldOf("Rot", 0f).forGetter(RotationData::rotation),
        Codec.FLOAT.optionalFieldOf("RotVal", 0f).forGetter(RotationData::rotationValue)
    ).apply(instance, RotationData::new));

    public static final GraphEntityType<RotationData> TYPE = GraphEntityType.of(id("rotation_info"), CODEC, RotationData::new, RotationData::split);
    public static RotationData EMPTY = new RotationData() {
        @Override
        public void update(ServerWorld world) {
        }
    };
    protected boolean overstressed = false;

    private double speed;
    private float rotationValue;
    private float rotation;
    private int lastTick = -1;
    private double stressCapacity;
    private double stressUsage;
    private GraphEntityContext ctx;
    private boolean negative;

    public RotationData() {
    }

    public RotationData(float rotation, float rotationValue) {
        this.rotation = rotation;
        this.rotationValue = rotationValue;
    }

    private static void collectGraphs(Collection<NodeHolder<GearMechanicalNode>> connectors, ObjectOpenHashSet<BlockPos> checkedNodes,
                                      Long2BooleanOpenHashMap dirMap, Long2FloatOpenHashMap speedMap, MutableBoolean clogged, ArrayList<RotationData> data,
                                      boolean currentDirection, float speed) {
        for (var connection : connectors) {
            //DebugInfoSender.addGameTestMarker((ServerWorld) connection.getBlockWorld(), connection.getBlockPos(), "Alt", 0x88FFFF66, 50 * 4 * 20);

            var connectGraph = Objects.requireNonNull(FactoryNodes.ROTATIONAL_CONNECTOR.getGraphWorld((ServerWorld) connection.getBlockWorld()))
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

            var subConnectors = targetGraph.getCachedNodes(GearMechanicalNode.CACHE);
            if (!subConnectors.isEmpty()) {
                collectGraphs(subConnectors, checkedSelf, dirMap, speedMap, clogged, data, nextDirection, nextGearSpeed);
            }
        }
    }

    public double speed() {
        return this.overstressed ? 0 : this.speed;
    }

    public double directSpeed() {
        return this.speed;
    }

    public double stressCapacity() {
        return this.overstressed ? 0 : this.stressCapacity;
    }
    public double directStressCapacity() {
        return stressCapacity;
    }

    public double stressUsage() {
        return this.overstressed ? 0 : this.stressUsage;
    }

    public double directStressUsage() {
        return this.stressUsage;
    }

    public float rotation() {
        return this.rotation;
    }

    public float rotationValue() {
        return this.rotationValue;
    }

    public boolean isNegative() {
        return this.negative;
    }

    public void update(ServerWorld world) {
        var currentTick = world.getServer().getTicks();
        if (this.ctx == null || this.lastTick == currentTick) {
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
            collectGraphs(connectors, checkedNodes, dirMap, speedMap, clogged, rotationDataList, false, 1);

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
                    rot.overstressed = true;
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


            for (var data : rotationDataList) {
                var stress = usage.get(data.getContext().getGraph().getId());
                fullStress += stress / speedMap.get(data.getContext().getGraph().getId());
            }

            var negative = state.speed == 0 ? TriState.DEFAULT : TriState.of(state.speed < 0);
            state.multiplier = 1;

            state.stressUsed = fullStress;

            float biggest = 1;
            for (var data : rotationDataList) {
                data.negative = negative == TriState.DEFAULT ? data.negative : dirMap.get(data.getContext().getGraph().getId()) != negative.get();
                state.multiplier = speedMap.get(data.getContext().getGraph().getId());
                biggest = Math.max(biggest, state.multiplier);
                data.applyState(state);
                if (data.overstressed) {
                    data.getContext().getGraph().getNodes().forEach(this::spawnSmoke);
                }
            }

            if (this.speed == 0 || Math.abs(state.stressCapacity) - fullStress < 0) {
                return;
            }

            var speed = this.speed;

            if (this.negative) {
                speed = -speed;
            }

            var baseDelta = speed * MathHelper.RADIANS_PER_DEGREE * delta;
            float r = Math.abs((float) ((baseDelta + this.rotationValue) % (MathHelper.TAU * biggest)));

            var rotMax = (RotationConstants.MAX_ROTATION_PER_TICK_3 - MathHelper.RADIANS_PER_DEGREE * 15) * delta;

            for (var data : rotationDataList) {
                var div = speedMap.get(data.getContext().getGraph().getId());
                data.rotationValue = (data.negative ? -r : r) / div;

                if (Math.abs(baseDelta / div) < rotMax) {
                    data.rotation = data.rotationValue;
                } else {
                    data.rotation = (data.rotation + (data.negative ? -rotMax : rotMax)) % MathHelper.TAU;
                }
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
            this.overstressed = false;
            this.stressCapacity = 0;
            this.stressUsage = 0;
            this.speed = 0;
            return;
        }
        this.stressCapacity = Math.abs(state.finalStressCapacity());
        this.stressUsage = state.finalStressUsed();
        this.overstressed = this.stressCapacity - stressUsage < 0;

        this.speed = Math.abs(state.finalSpeed() / state.providerCount);
    }

    private void updateSelf(ServerWorld world, float delta) {
        var graph = this.ctx.getGraph();
        var state = new State();
        calculateState(world, state);
        applyState(state);

        this.negative = state.speed == 0 ? this.negative : state.speed < 0;

        var speed = this.speed != 0 ? this.speed * MathHelper.sign(state.speed) : 0;

        if (this.overstressed) {
            for (var entry : graph.getCachedNodes(FunctionalDirectionNode.CACHE)) {
                var pos = entry.getBlockPos();
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.2);
            }
            return;
        }

        var baseDelta = speed * MathHelper.RADIANS_PER_DEGREE * delta;
        float r = (float) ((baseDelta + this.rotationValue) % MathHelper.TAU);

        var rotMax = (RotationConstants.MAX_ROTATION_PER_TICK_3 - MathHelper.RADIANS_PER_DEGREE * 15) * delta;

        this.rotationValue = (this.negative ? -r : r);

        if (Math.abs(baseDelta) < rotMax) {
            this.rotation = this.rotationValue;
        } else {
            this.rotation = (this.rotation + (this.negative ? -rotMax : rotMax)) % MathHelper.TAU;
        }
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
    public void merge(@NotNull RotationData other) {
        if (this.ctx.getGraph().size() < other.ctx.getGraph().size()) {
            this.rotation = other.rotation;
            this.rotationValue = other.rotationValue;
        } else {
            other.rotation = this.rotation;
            other.rotationValue = this.rotationValue;
        }
    }

    private @NotNull RotationData split(@NotNull BlockGraph originalGraph, @NotNull BlockGraph newGraph) {
        var data = new RotationData();
        data.rotation = this.rotation;
        data.rotationValue = this.rotationValue;
        data.speed = this.speed;
        return data;
    }

    public boolean isOverstressed() {
        return this.overstressed;
    }
    @Nullable
    public Text getStateText() {
        if (this.overstressed) {
            return this.stressCapacity == 0 ? MachineInfoProvider.LOCKED_TEXT : MachineInfoProvider.OVERSTRESSED_TEXT;
        }
        return null;
    }

    public Text getStateTextOrElse(Text fallback) {
        var state = getStateText();
        return state != null ? state : fallback;
    }

    public float speedRadians() {
        return (float) (this.speed * (this.negative ? -1 : 1) * MathHelper.RADIANS_PER_DEGREE);
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
            return this.stressUsed * this.multiplier;
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
