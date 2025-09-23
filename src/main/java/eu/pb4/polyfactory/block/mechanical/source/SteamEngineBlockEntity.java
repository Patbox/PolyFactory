package eu.pb4.polyfactory.block.mechanical.source;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.ui.FuelSlot;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SteamEngineBlockEntity extends LockableBlockEntity implements MinimalSidedInventory {
    private static final int[] SLOTS = new int[]{0, 1, 2};
    public float state = 0;
    public int fuelTicks = 0;
    public int fuelInitial = 1;
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(3, ItemStack.EMPTY);

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.STEAM_ENGINE, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        var self = (SteamEngineBlockEntity) t;

        if (self.fuelTicks > 0) {
            self.fuelTicks -= MathHelper.lerp(self.state, 40, 16);
            self.state = (float) Math.min(self.state + 0.005, 1);

            if (!state.get(SteamEngineBlock.LIT)) {
                world.setBlockState(pos, state.with(SteamEngineBlock.LIT, true));
            }
            self.markDirty();
            if ((world.getTime() + pos.getX() * 3L + pos.getY() * 7L + pos.getZ() * 5L) % MathHelper.clamp(Math.round(2 / self.state), 4, 8) == 0) {
                var facing = state.get(SteamEngineBlock.FACING);
                var a = new Vec3d(
                        pos.getX() + 0.5 + (facing.getAxis() == Direction.Axis.Z ? 0.5f : 0),
                        pos.getY() + 1,
                        pos.getZ() + 0.5 + (facing.getAxis() == Direction.Axis.X ? 0.5f : 0)
                );

                ((ServerWorld) world).spawnParticles(ParticleTypes.CLOUD, a.x + world.random.nextFloat() - 0.5, a.y + world.random.nextFloat() - 0.5, a.z + world.random.nextFloat() - 0.5, 0, 0, 1, 0, 0.1);
            }
        } else {
            for (int i = 0; i < 3; i++) {
                var stack = self.getStack(i);

                if (!stack.isEmpty()) {
                    var value = world.getFuelRegistry().getFuelTicks(stack);
                    if (value > 0) {
                        var remainder = ((FabricItemStack) (Object) stack).getRecipeRemainder();
                        stack.decrement(1);
                        self.fuelTicks = value * 10;
                        self.fuelInitial = self.fuelTicks;
                        if (stack.isEmpty()) {
                            self.setStack(i, ItemStack.EMPTY);
                        }

                        if (!remainder.isEmpty()) {
                            FactoryUtil.tryInsertingRegular(self, remainder);
                            if (!remainder.isEmpty()) {
                                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, remainder);
                            }
                        }

                        self.markDirty();
                        return;
                    }
                }
            }

            if (self.state != 0) {
                self.state = (float) Math.max(self.state - 0.02, 0);
                self.markDirty();
            }
            if (state.get(SteamEngineBlock.LIT)) {
                world.setBlockState(pos, state.with(SteamEngineBlock.LIT, false));
            }
        }

    }

    @Override
    protected void writeData(WriteView view) {
        Inventories.writeData(view, this.items);
        view.putInt("FuelTicks", this.fuelTicks);
        view.putInt("FuelInitial", this.fuelInitial);
        view.putFloat("State", this.state);
        super.writeData(view);
    }

    @Override
    public void readData(ReadView view) {
        Inventories.readData(view, items);
        this.fuelInitial = view.getInt("FuelInitial", 0);
        this.fuelTicks = view.getInt("FuelTicks", 0);
        this.state = view.getFloat("State", 0);
        super.readData(view);
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld serverWorld, BlockPos pos) {
        if (this.state > 0) {
            modifier.provide(30 * this.state, 110 * this.state, state.get(SteamEngineBlock.FACING).getDirection());
        }
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.items;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.world != null && this.world.getFuelRegistry().isFuel(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    public void createGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (this.world == null) {
            return;
        }

        var block = (SteamEngineBlock) this.getCachedState().getBlock();
        for (var pos : BlockPos.iterate(this.getPos().add(0, -1, 0),
                this.getPos().add(block.getMaxX(this.getCachedState()), 1, block.getMaxZ(this.getCachedState())))) {
            if (pos.equals(this.getPos())) {
                continue;
            }
            this.world.updateComparators(pos, this.getCachedState().getBlock());
        }
    }

    private class Gui extends SimpleGui {
        private boolean active;

        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X2, player, false);
            this.setTitle(GuiTextures.STEAM_ENGINE.apply(SteamEngineBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(9 + 3, new FuelSlot(SteamEngineBlockEntity.this, 0, player.getEntityWorld().getFuelRegistry()));
            this.setSlotRedirect(9 + 4, new FuelSlot(SteamEngineBlockEntity.this, 1, player.getEntityWorld().getFuelRegistry()));
            this.setSlotRedirect(9 + 5, new FuelSlot(SteamEngineBlockEntity.this, 2, player.getEntityWorld().getFuelRegistry()));
            this.setSlot(4, GuiTextures.FLAME.getCeil(progress()));
            this.active = SteamEngineBlockEntity.this.fuelTicks > 0;
            this.open();
        }

        private float progress() {
            return SteamEngineBlockEntity.this.fuelInitial > 0
                    ? MathHelper.clamp(SteamEngineBlockEntity.this.fuelTicks / (float) SteamEngineBlockEntity.this.fuelInitial, 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(SteamEngineBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }

            var active = SteamEngineBlockEntity.this.fuelTicks > 0;
            if (!this.active && active) {
                this.active = true;
                TriggerCriterion.trigger(this.player, FactoryTriggers.FUEL_STEAM_ENGINE);
            }
            this.setSlot(4, GuiTextures.FLAME.getCeil(progress()));
            super.onTick();
        }
    }
}
