package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public class EjectorBlockEntity extends BlockEntity implements BlockEntityExtraListener {
    @Nullable
    private EjectorBlock.Model model;

    private float angle = 45;

    private float progress = 0.5f;

    private float strength = 2;

    private long ignoredTick = -1;

    public EjectorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.EJECTOR, pos, state);
    }

    public static <T extends BlockEntity> void tick(World worldx, BlockPos pos, BlockState state, T t) {
        if (!(worldx instanceof ServerWorld world) || !(t instanceof EjectorBlockEntity be) || be.ignoredTick + 10 > world.getTime()) {
            return;
        }
        var speed = RotationUser.getRotation(world, pos).speed();
        be.setProgress((float) (be.progress + speed / (be.strength * be.strength * 60)));
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (this.progress < 1) {
            modifier.stress(this.strength * 1.2);
        }
    }


    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putFloat("angle", this.angle);
        view.putFloat("strength", this.strength);
        view.putFloat("progress", this.progress);
    }


    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.angle = view.getFloat("angle", 45);
        this.strength = view.getFloat("strength", 2);
        this.progress = view.getFloat("process", 0);
    }

    public void setAngle(float angle) {
        this.angle = angle;
        this.markDirty();
    }

    public float angle() {
        return this.angle;
    }

    public float progress() {
        return this.progress;
    }

    public float strength() {
        return this.strength;
    }

    public void setStrength(float strength) {
        var old = this.strength;
        this.strength = strength;
        setProgress(this.progress * old / strength);
        this.markDirty();
    }

    @Override
    public void onListenerUpdate(WorldChunk worldChunk) {
        this.model = (EjectorBlock.Model) BlockAwareAttachment.get(worldChunk, pos).holder();
    }

    public void setProgress(float progress) {
        this.progress = Math.min(progress, 1);
        this.markDirty();
        if (this.model != null) {
            this.model.updateProgress((1 - this.progress) * (60 - this.angle * 0.4f) * MathHelper.RADIANS_PER_DEGREE);
            this.model.tick();
        }
    }

    public void setIgnoredTick(long ignoredTick) {
        this.ignoredTick = ignoredTick;
    }

    public long ignoredTick() {
        return ignoredTick;
    }
}
