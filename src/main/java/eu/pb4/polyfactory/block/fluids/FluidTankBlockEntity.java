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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class FluidTankBlockEntity extends BlockEntity implements FluidInputOutput.ContainerBased, BlockEntityExtraListener {
    public static final long CAPACITY = FluidConstants.BLOCK * 6;
    @Nullable
    private FluidTankBlock.Model model;
    private boolean postInitialRead = false;
    private float blockTemperature = 0;
    private int disableUpdates = 0;
    private boolean delayedOnFluidChanges = false;
    private boolean requireComparatorUpdate = false;
    private final FluidContainerImpl container = new FluidContainerImpl(CAPACITY, this::onFluidChanged);

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FLUID_TANK, pos, state);
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof FluidTankBlockEntity tank)) {
            return;
        }
        tank.postInitialRead = true;
        updateVertical(world, tank, pos, state);
        updateHorizontal(world, tank, pos, state);
        updateVertical(world, tank, pos, state);


        var y = state.getValue(FluidTankBlock.PART_Y);
        if ((y.single() || y.positive()) && tank.model != null) {
            tank.model.setFluidAbove(null);
        }
        if ((y.single() || y.negative()) && tank.model != null) {
            tank.model.setFluidBelow(null);
        }
        FluidContainerUtil.tick(tank.container, (ServerLevel) world, pos, tank.blockTemperature, tank::dropItem);

        if (tank.requireComparatorUpdate) {
            world.updateNeighbourForOutputSignal(pos, state.getBlock());
            tank.requireComparatorUpdate = false;
        }

        tank.updateModel();
    }

    private static void updateVertical(Level world, FluidTankBlockEntity tank, BlockPos pos, BlockState state) {
        var y = state.getValue(FluidTankBlock.PART_Y);
        if ((y.middle() || y.positive()) && world.getBlockEntity(pos.relative(Direction.DOWN)) instanceof FluidTankBlockEntity below) {
            tank.disableUpdates++;
            below.disableUpdates++;
            try {
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
            } finally {
                tank.disableUpdates--;
                below.disableUpdates--;
                tank.runDelayedOnFluidChanges();
                below.runDelayedOnFluidChanges();
            }
        } else {
            tank.blockTemperature = BlockHeat.getReceived(world, pos) + tank.container.fluidTemperature();
        }
    }

    private static void updateHorizontal(Level world, FluidTankBlockEntity tank, BlockPos pos, BlockState state) {
        var p = new BlockPos.MutableBlockPos();

        var x = state.getValue(FluidTankBlock.PART_X);
        var z = state.getValue(FluidTankBlock.PART_Z);
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
                    tankk.disableUpdates++;
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

                for (var tankk : tanks) {
                    tankk.disableUpdates--;
                    tankk.runDelayedOnFluidChanges();
                }
            }
        }
    }

    public FluidContainer getFluidContainer() {
        return this.container;
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        this.container.writeData(view, "fluid");
        view.putBoolean("require_comparator_update", this.requireComparatorUpdate);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.container.readData(view, "fluid");
        this.requireComparatorUpdate = view.getBooleanOr("require_comparator_update", false);
        if (!this.postInitialRead) {
            updateModel();
            this.postInitialRead = true;
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var f = components.get(FactoryDataComponents.FLUID);
        if (f != null) {
            this.container.clear();
            f.extractTo(this.container);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
        componentMapBuilder.set(FactoryDataComponents.FLUID, FluidComponent.copyFrom(this.container));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard("fluid");
    }

    @Override
    public long insertFluid(FluidInstance<?> type, long amount, Direction direction) {
        if (this.level == null) {
            return FluidInputOutput.ContainerBased.super.insertFluid(type, amount, direction);
        }
        var mut = this.worldPosition.mutable();
        var tank = this;
        while (amount != 0) {
            amount = tank.container.insert(type, amount, false);
            if (tank.getBlockState().getValue(FluidTankBlock.PART_Y).hasNext() && tank.level.getBlockEntity(mut.move(Direction.UP)) instanceof FluidTankBlockEntity tank2) {
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
        this.requireComparatorUpdate = true;
        if (this.disableUpdates == 0) {
            if (this.level != null) {
                level.blockEntityChanged(this.getBlockPos());
            }
        } else {
            this.delayedOnFluidChanges = true;
        }
    }

    private void runDelayedOnFluidChanges() {
        if (this.delayedOnFluidChanges && this.disableUpdates == 0) {
            this.delayedOnFluidChanges = false;
            this.onFluidChanged();
        }
    }

    private void updateModel() {
        if (this.model != null) {
            this.model.setFluids(this.container);
        }
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        var x = BlockAwareAttachment.get(chunk, worldPosition);
        if (x != null && x.holder() instanceof FluidTankBlock.Model model) {
            this.model = model;
        }
    }

    private void dropItem(ItemStack stack) {
        Containers.dropItemStack(level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, stack);
    }
}
