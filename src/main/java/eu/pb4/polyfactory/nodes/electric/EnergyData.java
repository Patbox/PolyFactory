package eu.pb4.polyfactory.nodes.electric;

import com.kneelawk.graphlib.api.graph.BlockGraph;
import com.kneelawk.graphlib.api.graph.GraphEntityContext;
import com.kneelawk.graphlib.api.graph.user.GraphEntity;
import com.kneelawk.graphlib.api.graph.user.GraphEntityType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.block.electric.EnergyUser;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.pb4.polyfactory.ModInit.id;

public class EnergyData implements GraphEntity<EnergyData> {
    public static EnergyData EMPTY = new EnergyData() {
        @Override
        public void update(ServerWorld world, BlockGraph graph) {}
    };

    public static final GraphEntityType<EnergyData> TYPE = GraphEntityType.of(id("energy_info"), MapCodec.unit(EnergyData::new).codec(), EnergyData::new,  EnergyData::split);

    private long speed;
    private int lastTick = -1;
    private long stressCapacity;
    private GraphEntityContext ctx;

    public EnergyData() {
    }

    public long current() {
        return this.speed;
    }

    public double stressCapacity() {
        return stressCapacity;
    }

    public void update(ServerWorld world, BlockGraph graph) {
        var currentTick = world.getServer().getTicks();
        if (this.lastTick == currentTick) {
            return;
        }
        this.lastTick = currentTick;

        var list = graph.getCachedNodes(FunctionalDirectionNode.CACHE);

        if (list.isEmpty()) {
            this.speed = 0;
            this.stressCapacity = 0;
            return;
        }

        var state = new State(this);
        for (var entries : list) {
            var blockState = world.getBlockState(entries.getBlockPos());

            if (entries.getNode().getTargetFunctional(world, entries.getBlockPos(), blockState) instanceof EnergyUser rotationalSource) {
                rotationalSource.updateEnergyData(state, blockState, world, entries.getBlockPos());
            }
        }

        if (state.count == 0) {
            this.stressCapacity = 0;
            this.speed = 0;
            return;
        }

        state.stressCapacity += state.power;
        if (state.stressCapacity <= 0) {
            this.stressCapacity = 0;
            this.speed = 0;

            //for (var entry : list) {
            //    var pos = entry.getBlockPos();
            //    world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.2);
            //}
        } else {
            this.speed = state.power;
            this.stressCapacity = state.stressCapacity;
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
    public void onUnload() {

    }

    @Override
    public void merge(@NotNull EnergyData other) {
    }

    private @NotNull EnergyData split(@NotNull BlockGraph originalGraph, @NotNull BlockGraph newGraph) {
        var data = new EnergyData();
        data.speed = this.speed;
        return data;
    }

    public boolean powered() {
        return this.speed > 0;
    }

    public static class State {
        public final EnergyData data;
        protected long power;
        protected long stressCapacity;
        protected int count;


        public State(EnergyData data) {
            this.data = data;
        }
        public void provide(long value) {
            this.power += value;
            this.count++;
        }

        public void use(long value) {
            this.stressCapacity -= value;
        }
    }
}
