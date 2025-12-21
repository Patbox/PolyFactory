package eu.pb4.polyfactory.block.fluids;

import com.mojang.authlib.GameProfile;
import eu.pb4.factorytools.api.block.OwnedBlockEntity;
import eu.pb4.factorytools.api.util.LegacyNbtHelper;
import eu.pb4.polyfactory.advancement.FluidShootsCriterion;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.shooting.ShooterContext;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NozzleBlockEntity extends BlockEntity implements FluidInput.ContainerBased, ShooterContext, OwnedBlockEntity {
    protected final FluidContainerImpl container = new FluidContainerImpl(FluidConstants.BLOCK * 3 / 2, this::setChanged);
    private double speed;
    @Nullable
    private FluidInstance<?> currentFluid;
    private GameProfile owner;
    private int tick = 0;
    private float extraSpread = 0;

    public NozzleBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.NOZZLE, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putDouble("speed", this.speed);
        view.putInt("tick", this.tick);
        this.container.writeData(view, "fluid");
        view.putFloat("spread", this.extraSpread);
        if (this.currentFluid != null) {
            view.store("current_fluid", FluidInstance.CODEC, this.currentFluid);
        }
        if (this.owner != null) {
            view.store("owner", CompoundTag.CODEC, LegacyNbtHelper.writeGameProfile(new CompoundTag(), this.owner));
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.speed = view.getDoubleOr("speed", 0);
        this.tick = view.getIntOr("tick", 0);
        this.container.readData(view, "fluid");
        this.currentFluid = view.read("current_fluid", FluidInstance.CODEC).orElse(null);
        this.extraSpread = view.getFloatOr("spread", 0);
        view.read("owner", CompoundTag.CODEC).ifPresent(x -> this.owner = LegacyNbtHelper.toGameProfile(x));
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof NozzleBlockEntity nozzle)) {
            return;
        }
        var goodDir = nozzle.getBlockState().getValue(NozzleBlock.FACING);

        if (!nozzle.world().getChunkSource().chunkMap.getDistanceManager().inEntityTickingRange(ChunkPos.asLong(pos.relative(goodDir)))) {
            return;
        }

        if (nozzle.container.isEmpty()) {
            nozzle.stopShooting();
            if (nozzle.speed != 0) {
                nozzle.speed = 0;
                nozzle.setChanged();
            }
            return;
        }
        var num = new MutableDouble();
        NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPushFlows(pos, () -> true, (dir, strength) -> {
            if (dir == goodDir) {
                num.add(strength);
            } else {
                num.subtract(strength);
            }
        });
        var strength = num.doubleValue();
        if (strength <= 0.01) {
            nozzle.speed = 0;
            nozzle.stopShooting();
            return;
        }
        if (nozzle.speed != strength) {
            nozzle.speed = strength;
            nozzle.setChanged();
        }

        if (nozzle.currentFluid != null) {
            //noinspection unchecked
            var fluid = (FluidInstance<Object>) nozzle.currentFluid;
            if (fluid.shootingBehavior().canShoot(nozzle, fluid, nozzle.container)) {
                fluid.shootingBehavior().continueShooting(nozzle, fluid, nozzle.tick++, nozzle.container);
                nozzle.setChanged();
                return;
            }

            nozzle.stopShooting();
        }

        for (var f : nozzle.container.fluids()) {
            //noinspection unchecked
            var fluid = (FluidInstance<Object>) f;
            if (fluid.shootingBehavior().canShoot(nozzle, fluid, nozzle.container)) {
                nozzle.currentFluid = fluid;
                nozzle.tick = 0;
                fluid.shootingBehavior().startShooting(nozzle, fluid, nozzle.container);
                if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayer player) {
                    FluidShootsCriterion.triggerNozzle(player, fluid);
                }
                nozzle.setChanged();
                break;
            }
        }
    }

    private void stopShooting() {
        if (this.currentFluid == null) {
            return;
        }
        //noinspection unchecked
        var fluid = (FluidInstance<Object>) this.currentFluid;
        fluid.shootingBehavior().stopShooting(this, fluid);
        this.tick = 0;
        this.currentFluid = null;
        this.setChanged();
    }

    @Override
    public @Nullable FluidContainer getFluidContainer(Direction direction) {
        return direction == this.getBlockState().getValue(NozzleBlock.FACING).getOpposite() ? this.getMainFluidContainer() : null;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    @Override
    public RandomSource random() {
        return this.level.random;
    }

    @Override
    public ServerLevel world() {
        return (ServerLevel) this.level;
    }

    @Override
    public UUID uuid() {
        return this.owner != null ? this.owner.id() : Util.NIL_UUID;
    }

    @Override
    public Vec3 position() {
        return Vec3.atCenterOf(this.worldPosition).relative(this.getBlockState().getValue(NozzleBlock.FACING), 0.75);
    }

    @Override
    public Vec3 rotation() {
        return new Vec3(this.getBlockState().getValue(NozzleBlock.FACING).step());
    }

    @Override
    public SoundSource soundCategory() {
        return SoundSource.BLOCKS;
    }

    @Override
    public GameProfile getOwner() {
        return this.owner;
    }

    @Override
    public float extraSpread() {
        return this.extraSpread;
    }

    public void setExtraSpread(float extraSpread) {
        this.extraSpread = extraSpread;
        this.setChanged();
    }

    @Override
    public void setOwner(GameProfile profile) {
        this.owner = profile;
        this.setChanged();
    }

    @Override
    public float force() {
        return (float) Mth.clamp(this.speed * 10, 0.4, 1);
    }
}
