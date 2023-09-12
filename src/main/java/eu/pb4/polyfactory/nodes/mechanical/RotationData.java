package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.GraphEntity;
import com.kneelawk.graphlib.api.graph.user.GraphEntityType;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

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
            var clogged = new MutableBoolean();
            var checked = new LongOpenHashSet();
            var checked2 = new LongOpenHashSet();
            var rotationDataList = new ArrayList<RotationData>();
            dirMap.put(this.ctx.getGraph().getId(), false);
            checked.add(this.ctx.getGraph().getId());
            rotationDataList.add(this);
            collectGraphs(connectors, dirMap, clogged, checked, checked2, rotationDataList, false);
            if (rotationDataList.isEmpty()) {
                return;
            }

            if (clogged.booleanValue()) {
                for (var rot : rotationDataList) {
                    rot.lastTick = currentTick;
                    rot.speed = 0;
                    rot.stressUsage = 0;
                    rot.stressCapacity = 0;
                    rot.getContext().getGraph().getNodes().forEach((entry) -> {
                        var pos = entry.getBlockPos();
                        world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.2);
                    });
                }
                return;
            }

            var state = new State();
            for (var data : rotationDataList) {
                data.lastTick = currentTick;
                state.flip = dirMap.get(data.getContext().getGraph().getId());
                data.calculateState(world, state);
            }

            var negative = state.speed < 0;

            for (var data : rotationDataList) {
                data.applyState(state);
                data.negative = dirMap.get(data.getContext().getGraph().getId()) != negative;
            }

            var speed = this.speed;
            if (this.negative) {
                speed = -speed;
            }

            float r;

            if (speed == 0) {
                r = this.rotation;
            } else {
                r = (float) ((Math.min(speed * MathHelper.RADIANS_PER_DEGREE * delta, RotationConstants.MAX_ROTATION_PER_TICK_4 * delta) + this.rotation) % MathHelper.TAU);
            }
            if (this.negative) {
                r = -r;
            }


            for (var data : rotationDataList) {
                data.rotation = data.negative ? -r : r;
            }
        }
    }

    private static void collectGraphs(Collection<NodeHolder<AxleWithGearMechanicalNode>> connectors, Long2BooleanOpenHashMap dirMap, MutableBoolean clogged,
                                      LongOpenHashSet checked, LongOpenHashSet checked2, ArrayList<RotationData> data, boolean currentDirection) {
        for (var connection : connectors) {
            var connectGraph = FactoryNodes.ROTATIONAL_CONNECTOR.getSidedGraphView(connection.getBlockWorld()).getAllGraphsAt(connection.getBlockPos()).findFirst();
            if (connectGraph.isPresent() && checked2.add(connectGraph.get().getId())) {
                var iterator = connectGraph.get().getNodes().iterator();
                while (iterator.hasNext()) {
                    var x = iterator.next();
                    var optionalGraph = connection.getGraphWorld().getAllGraphsAt(x.getBlockPos()).findFirst();

                    if (optionalGraph.isEmpty() || !checked.add(optionalGraph.get().getId())) {
                        return;
                    }

                    var targetGraph = optionalGraph.get();
                    data.add(targetGraph.getGraphEntity(TYPE));

                    if (dirMap.containsKey(targetGraph.getId())) {
                        if (dirMap.get(targetGraph.getId()) == currentDirection) {
                            clogged.setTrue();
                        }
                    } else {
                        dirMap.put(targetGraph.getId(), !currentDirection);
                    }
                    var subConnectors = targetGraph.getCachedNodes(AxleWithGearMechanicalNode.CACHE);
                    if (!subConnectors.isEmpty()) {
                        collectGraphs(subConnectors, dirMap, clogged, checked, checked2, data, !currentDirection);
                    }
                }
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
        if (state.count == 0) {
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
            this.speed = Math.abs(state.speed / state.count);
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

        this.rotation = (float) ((Math.min(speed * MathHelper.RADIANS_PER_DEGREE * delta, RotationConstants.MAX_ROTATION_PER_TICK_4 * delta) + this.rotation) % MathHelper.TAU);
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
        public boolean flip = false;
        protected double speed;
        protected double stressCapacity;
        protected double stressUsed;
        protected int count;

        public void provide(double speed, double stressCapacity, boolean negative) {
            this.count++;

            if (this.flip == negative) {
                this.speed += speed;
                this.stressCapacity += stressCapacity;
            } else {
                this.speed -= speed;
                this.stressCapacity -= stressCapacity;
            }
        }

        public void stress(double stress) {
            this.stressUsed += stress;
        }

        public void provide(float speed, float stressCapacity, Direction.AxisDirection direction) {
            provide(speed, stressCapacity, direction == Direction.AxisDirection.NEGATIVE);
        }
    }
}
