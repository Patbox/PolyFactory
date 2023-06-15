package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.user.GraphEntity;
import com.kneelawk.graphlib.api.graph.user.GraphEntityType;
import eu.pb4.polyfactory.block.mechanical.RotationalSource;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.pb4.polyfactory.ModInit.id;

public class RotationData implements GraphEntity<RotationData> {
    public static RotationData EMPTY = new RotationData() {
        @Override
        public void update(ServerWorld world, BlockGraph graph) {

        }
    };

    public static final GraphEntityType<RotationData> TYPE = new GraphEntityType<>(id("rotation_info"),
            RotationData::create, RotationData::decode, RotationData::split);

    private double speed;
    private float rotation;
    private int lastTick = -1;

    public RotationData() {
    }

    public double speed() {
        return this.speed;
    }

    public float rotation() {
        return this.rotation;
    }

    public void update(ServerWorld world, BlockGraph graph) {
        if (this.lastTick == world.getServer().getTicks()) {
            return;
        }
        this.lastTick = world.getServer().getTicks();

        var list = graph.getCachedNodes(RotationalSourceNode.CACHE);

        if (list.size() == 0) {
            this.speed = 0;
            return;
        }

        var speed = 0d;

        for (var entries : list) {
            var state = world.getBlockState(entries.getPos());

            if (state.getBlock() instanceof RotationalSource rotationalSource) {
                speed += rotationalSource.getSpeed(state, world, entries.getPos());
            }
        }

        this.speed = speed / list.size();
        var r = (float) (this.speed + this.rotation);
        if (r > MathHelper.TAU) {
            r -= MathHelper.TAU;
        }
        this.rotation = r;
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
    public void onUnload() {

    }

    @Override
    public void merge(@NotNull RotationData other) {
        if (this.speed < other.speed) {
            this.rotation = other.rotation;
        }

    }

    private RotationData split(@NotNull BlockGraph blockGraph, @NotNull GraphEntityContext graphEntityContext) {
        var data = new RotationData();
        data.rotation = this.rotation;
        data.speed = this.speed;
        return data;
    }

    private static RotationData decode(@Nullable NbtElement nbtElement, @NotNull GraphEntityContext graphEntityContext) {
        return new RotationData();
    }

    private static RotationData create(@NotNull GraphEntityContext graphEntityContext) {
        return new RotationData();
    }
}
