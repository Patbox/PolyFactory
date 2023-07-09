package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.user.GraphEntity;
import com.kneelawk.graphlib.api.graph.user.GraphEntityType;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.pb4.polyfactory.ModInit.id;

public class RotationData implements GraphEntity<RotationData> {
    public static RotationData EMPTY = new RotationData() {
        @Override
        public void update(ServerWorld world, BlockGraph graph) {}
    };

    public static final GraphEntityType<RotationData> TYPE = GraphEntityType.of(id("rotation_info"), RotationData::new, RotationData::decode, RotationData::split);

    private double speed;
    private float rotation;
    private int lastTick = -1;
    private double stressCapacity;
    private GraphEntityContext ctx;

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

    public void update(ServerWorld world, BlockGraph graph) {
        var currentTick = world.getServer().getTicks();
        if (this.lastTick == currentTick) {
            return;
        }
        var delta = this.lastTick == -1 ? 1 : currentTick - this.lastTick;
        this.lastTick = currentTick;

        var list = graph.getCachedNodes(DirectionalRotationUserNode.CACHE);

        if (list.size() == 0) {
            this.speed = 0;
            this.stressCapacity = 0;
            return;
        }

        var state = new State();
        for (var entries : list) {
            var blockState = world.getBlockState(entries.getBlockPos());

            if (blockState.getBlock() instanceof RotationUser rotationalSource) {
                rotationalSource.updateRotationalData(state, blockState, world, entries.getBlockPos());
            }
        }

        if (state.count == 0) {
            this.stressCapacity = 0;
            this.speed = 0;
            return;
        }

        if (state.stressCapacity < 0) {
            this.stressCapacity = 0;
            this.speed = 0;

            for (var entry : list) {
                var pos = entry.getBlockPos();
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.2);
            }

            return;
        } else {
            this.speed = state.speed / state.count;
            this.stressCapacity = state.stressCapacity;
        }

        var r = (float) (Math.min(this.speed * MathHelper.RADIANS_PER_DEGREE * delta, RotationConstants.MAX_ROTATION_PER_TICK_4 * delta) + this.rotation);
        while (r > MathHelper.TAU) {
            r -= MathHelper.TAU;
        }

        this.rotation = r;
    }

    @Override
    public void onInit(@NotNull GraphEntityContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public @NotNull GraphEntityContext getContext() {
        return null;
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
        protected double speed;
        protected double stressCapacity;
        protected int count;

        public void provide(double speed, double stressCapacity) {
            this.speed += speed;
            this.stressCapacity += stressCapacity;
            this.count++;
        }

        public void stress(double stress) {
            this.stressCapacity -= stress;
        }
    }
}
