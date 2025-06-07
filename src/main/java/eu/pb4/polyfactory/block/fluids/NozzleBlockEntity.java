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
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NozzleBlockEntity extends BlockEntity implements FluidInput.ContainerBased, ShooterContext, OwnedBlockEntity {
    protected final FluidContainerImpl container = new FluidContainerImpl(FluidConstants.BLOCK * 3 / 2, this::markDirty);
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
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putDouble("speed", this.speed);
        view.putInt("tick", this.tick);
        this.container.writeData(view, "fluid");
        view.putFloat("spread", this.extraSpread);
        if (this.currentFluid != null) {
            view.put("current_fluid", FluidInstance.CODEC, this.currentFluid);
        }
        if (this.owner != null) {
            view.put("owner", NbtCompound.CODEC, LegacyNbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.speed = view.getDouble("speed", 0);
        this.tick = view.getInt("tick", 0);
        this.container.readData(view, "fluid");
        this.currentFluid = view.read("current_fluid", FluidInstance.CODEC).orElse(null);
        this.extraSpread = view.getFloat("spread", 0);
        view.read("owner", NbtCompound.CODEC).ifPresent(x -> this.owner = LegacyNbtHelper.toGameProfile(x));
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof NozzleBlockEntity nozzle)) {
            return;
        }
        var goodDir = nozzle.getCachedState().get(NozzleBlock.FACING);

        if (!nozzle.world().getChunkManager().chunkLoadingManager.getLevelManager().shouldTickEntities(ChunkPos.toLong(pos.offset(goodDir)))) {
            return;
        }

        if (nozzle.container.isEmpty()) {
            nozzle.stopShooting();
            if (nozzle.speed != 0) {
                nozzle.speed = 0;
                nozzle.markDirty();
            }
            return;
        }
        var num = new MutableDouble();
        NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPushFlows(pos, () -> true, (dir, strength) -> {
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
            nozzle.markDirty();
        }

        if (nozzle.currentFluid != null) {
            //noinspection unchecked
            var fluid = (FluidInstance<Object>) nozzle.currentFluid;
            if (fluid.shootingBehavior().canShoot(nozzle, fluid, nozzle.container)) {
                fluid.shootingBehavior().continueShooting(nozzle, fluid, nozzle.tick++, nozzle.container);
                nozzle.markDirty();
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
                if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayerEntity player) {
                    FluidShootsCriterion.triggerNozzle(player, fluid);
                }
                nozzle.markDirty();
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
        this.markDirty();
    }

    @Override
    public @Nullable FluidContainer getFluidContainer(Direction direction) {
        return direction == this.getCachedState().get(NozzleBlock.FACING).getOpposite() ? this.getMainFluidContainer() : null;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    @Override
    public Random random() {
        return this.world.random;
    }

    @Override
    public ServerWorld world() {
        return (ServerWorld) this.world;
    }

    @Override
    public UUID uuid() {
        return this.owner != null ? this.owner.getId() : Util.NIL_UUID;
    }

    @Override
    public Vec3d position() {
        return Vec3d.ofCenter(this.pos).offset(this.getCachedState().get(NozzleBlock.FACING), 0.75);
    }

    @Override
    public Vec3d rotation() {
        return new Vec3d(this.getCachedState().get(NozzleBlock.FACING).getUnitVector());
    }

    @Override
    public SoundCategory soundCategory() {
        return SoundCategory.BLOCKS;
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
        this.markDirty();
    }

    @Override
    public void setOwner(GameProfile profile) {
        this.owner = profile;
        this.markDirty();
    }

    @Override
    public float force() {
        return (float) MathHelper.clamp(this.speed * 10, 0.4, 1);
    }
}
