package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class FluidTankBlockEntity extends BlockEntity implements FluidInputOutput.ContainerBased, BlockEntityExtraListener {
    public static final long CAPACITY = FluidConstants.BLOCK * 6;
    private final FluidContainerImpl container = new FluidContainerImpl(CAPACITY, this::onFluidChanged);

    @Nullable
    private FluidTankBlock.Model model;
    private boolean postInitialRead = false;
    private float blockTemperature = 0;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FLUID_TANK, pos, state);
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("fluid", this.container.toNbt(registryLookup));
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.container.fromNbt(registryLookup, nbt, "fluid");
        if (!this.postInitialRead) {
            updateModel();
            this.postInitialRead = true;
        }
    }
    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        var f = components.get(FactoryDataComponents.FLUID);
        if (f != null) {
            this.container.clear();
            f.extractTo(this.container);
        }
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        componentMapBuilder.add(FactoryDataComponents.FLUID, FluidComponent.copyFrom(this.container));
    }

    @Override
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        nbt.remove("fluid");
    }

    @Override
    public long insertFluid(FluidInstance<?> type, long amount, Direction direction) {
        if (this.world == null) {
            return FluidInputOutput.ContainerBased.super.insertFluid(type, amount, direction);
        }
        var mut = this.pos.mutableCopy();
        var tank = this;
        while (amount != 0) {
            amount = tank.container.insert(type, amount, false);
            if (tank.getCachedState().get(FluidTankBlock.PART_Y).hasNext() && tank.world.getBlockEntity(mut.move(Direction.UP)) instanceof FluidTankBlockEntity tank2) {
                tank = tank2;
            } else {
                break;
            }
        }

        return amount;
    }

    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.container;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    private void onFluidChanged() {
        this.markDirty();
    }

    private void updateModel() {
        if (this.model != null) {
            this.model.setFluids(this.container);
        }
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, pos);
        if (x != null && x.holder() instanceof FluidTankBlock.Model model) {
            this.model = model;
        }
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof FluidTankBlockEntity tank)) {
            return;
        }
        tank.postInitialRead = true;
        updateVertical(world, tank, pos, state);
        updateHorizontal(world, tank, pos, state);
        updateVertical(world, tank, pos, state);


        var y = state.get(FluidTankBlock.PART_Y);
        if ((y.single() || y.positive()) && tank.model != null) {
            tank.model.setFluidAbove(null);
        }
        if ((y.single() || y.negative()) && tank.model != null) {
            tank.model.setFluidBelow(null);
        }
        FluidContainerUtil.tick(tank.container, (ServerWorld) world, pos, tank.blockTemperature, tank::dropItem);
        tank.updateModel();
    }

    private void dropItem(ItemStack stack) {
        ItemScatterer.spawn(world, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, stack);
    }

    private static void updateVertical(World world, FluidTankBlockEntity tank, BlockPos pos, BlockState state) {
        var y = state.get(FluidTankBlock.PART_Y);
        if ((y.middle() || y.positive()) && world.getBlockEntity(pos.offset(Direction.DOWN)) instanceof FluidTankBlockEntity below) {
            tank.blockTemperature = below.blockTemperature;
            if (below.container.isNotFull()) {
                while (below.container.isNotFull() && tank.container.isNotEmpty()) {
                    var ownBottomFluid = tank.container.bottomFluid();
                    below.container.insert(ownBottomFluid, tank.container.extract(ownBottomFluid, below.container.empty(), false), false);
                }
            }

            var ownBottomFluid = tank.container.bottomFluid();
            if (ownBottomFluid != null) {
                var belowTop = below.container.topFluid();
                assert belowTop != null;

                if (belowTop.density() < ownBottomFluid.density()) {
                    var swappable = Math.min(below.container.get(belowTop), tank.container.get(ownBottomFluid));
                    below.container.extract(belowTop, swappable, false);
                    tank.container.extract(ownBottomFluid, swappable, false);
                    below.container.insert(ownBottomFluid, swappable, false);
                    tank.container.insert(belowTop, swappable, false);
                }
            }

            if (below.model != null) {
                below.model.setFluidAbove(tank.container.bottomFluid());
            }
            if (tank.model != null) {
                tank.model.setFluidBelow(below.container.topFluid());
            }
        } else {
            tank.blockTemperature = BlockHeat.getReceived(world, pos) + tank.container.fluidTemperature();
        }
    }

    private static void updateHorizontal(World world, FluidTankBlockEntity tank, BlockPos pos, BlockState state) {
        var p = new BlockPos.Mutable();

        var x = state.get(FluidTankBlock.PART_X);
        var z = state.get(FluidTankBlock.PART_Z);
        if (!x.single() || !z.single()) {
            var tanks = new ArrayList<FluidTankBlockEntity>(5);
            tanks.add(tank);
            if ((x.middle() || x.positive()) && world.getBlockEntity(p.set(pos).move(-1, 0, 0)) instanceof FluidTankBlockEntity tank2) {
                tanks.add(tank2);
            }
            if ((x.middle() || x.negative()) && world.getBlockEntity(p.set(pos).move(1, 0, 0)) instanceof FluidTankBlockEntity tank2) {
                tanks.add(tank2);
            }
            if ((z.middle() || z.positive()) && world.getBlockEntity(p.set(pos).move(0, 0, -1)) instanceof FluidTankBlockEntity tank2) {
                tanks.add(tank2);
            }
            if ((z.middle() || z.negative()) && world.getBlockEntity(p.set(pos).move(0, 0, 1)) instanceof FluidTankBlockEntity tank2) {
                tanks.add(tank2);
            }


            if (tanks.size() > 1) {
                var map = new Object2LongOpenHashMap<FluidInstance<?>>();

                for (var tankk : tanks) {
                    tankk.container.forEach((a, b) -> map.put(a, map.getLong(a) + b));
                    tankk.container.clear();
                }

                var leftover = new Object2LongOpenHashMap<FluidInstance<?>>(map.size());
                for (var key : map.keySet()) {
                    var val = map.getLong(key);
                    var split = val / tanks.size();

                    map.put(key, split);
                    leftover.put(key, val - split * tanks.size());
                }

                for (var tankk : tanks) {
                    tankk.container.clear();
                    for (var val : map.keySet()) {
                        tankk.container.set(val, map.getLong(val));
                    }
                }
                for (var val : leftover.keySet()) {
                    tank.container.insert(val, leftover.getLong(val), false);
                }
            }
        }
    }
}
