package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

    public static <T extends BlockEntity> void tick(Level worldx, BlockPos pos, BlockState state, T t) {
        if (!(worldx instanceof ServerLevel world) || !(t instanceof EjectorBlockEntity be) || be.ignoredTick + 10 > world.getGameTime()) {
            return;
        }
        var speed = RotationUser.getRotation(world, pos).speed();
        be.setProgress((float) (be.progress + speed / (be.strength * be.strength * 60)));
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (this.progress < 1) {
            modifier.stress(this.strength * 1.2);
        }
    }


    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putFloat("angle", this.angle);
        view.putFloat("strength", this.strength);
        view.putFloat("progress", this.progress);
    }


    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.angle = view.getFloatOr("angle", 45);
        this.strength = view.getFloatOr("strength", 2);
        this.progress = view.getFloatOr("process", 0);
    }

    public void setAngle(float angle) {
        this.angle = angle;
        this.setChanged();
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
        this.setChanged();
    }

    @Override
    public void onListenerUpdate(LevelChunk worldChunk) {
        this.model = (EjectorBlock.Model) BlockAwareAttachment.get(worldChunk, worldPosition).holder();
    }

    public void setProgress(float progress) {
        this.progress = Math.min(progress, 1);
        this.setChanged();
        if (this.model != null) {
            this.model.updateProgress((1 - this.progress) * (60 - this.angle * 0.4f) * Mth.DEG_TO_RAD);
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
