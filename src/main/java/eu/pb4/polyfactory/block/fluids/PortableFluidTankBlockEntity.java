package eu.pb4.polyfactory.block.fluids;

import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
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
import org.jetbrains.annotations.Nullable;

public class PortableFluidTankBlockEntity extends BlockEntity implements FluidInputOutput.ContainerBased {
    public static final long CAPACITY = FluidConstants.BLOCK * 3;
    private final FluidContainer container = new FluidContainer(CAPACITY, this::onFluidChanged);
    private float blockTemperature = 0;

    public PortableFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PORTABLE_FLUID_TANK, pos, state);
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
    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof PortableFluidTankBlockEntity tank)) {
            return;
        }
        tank.blockTemperature = BlockHeat.get(world.getBlockState(pos.down())) + tank.container.fluidTemperature();
        tank.container.tick((ServerWorld) world, pos, tank.blockTemperature, tank::dropItem);
    }

    private void dropItem(ItemStack stack) {
        ItemScatterer.spawn(world, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, stack);
    }
}
