package eu.pb4.polyfactory.block.mechanical.source;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.ui.FuelSlot;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.RedstoneActivationType;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SteamEngineBlockEntity extends LockableBlockEntity implements MinimalSidedContainer {
    private static final int[] SLOTS = new int[]{0, 1, 2};
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    public float state = 0;
    public int fuelTicks = 0;
    public int fuelInitial = 1;
    private int redstoneState = 0;

    private RedstoneActivationType activationType = RedstoneActivationType.ALWAYS;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.STEAM_ENGINE, pos, state);
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        var self = (SteamEngineBlockEntity) t;

        if (self.fuelTicks > 0) {
            self.fuelTicks -= Mth.lerpInt(self.state, 40, 16);
            self.state = (float) Math.min(self.state + 0.005, 1);

            if (!state.getValue(SteamEngineBlock.LIT)) {
                world.setBlockAndUpdate(pos, state.setValue(SteamEngineBlock.LIT, true));
            }
            self.setChanged();
            if ((world.getGameTime() + pos.getX() * 3L + pos.getY() * 7L + pos.getZ() * 5L) % Mth.clamp(Math.round(2 / self.state), 4, 8) == 0) {
                var facing = state.getValue(SteamEngineBlock.FACING);
                var a = new Vec3(
                        pos.getX() + 0.5 + (facing.getAxis() == Direction.Axis.Z ? 0.5f : 0),
                        pos.getY() + 1,
                        pos.getZ() + 0.5 + (facing.getAxis() == Direction.Axis.X ? 0.5f : 0)
                );

                ((ServerLevel) world).sendParticles(ParticleTypes.CLOUD, a.x + world.random.nextFloat() - 0.5, a.y + world.random.nextFloat() - 0.5, a.z + world.random.nextFloat() - 0.5, 0, 0, 1, 0, 0.1);
            }
        } else {
            if (self.activationType.isActive(self.redstoneState != 0)) {
                for (int i = 0; i < 3; i++) {
                    var stack = self.getItem(i);

                    if (!stack.isEmpty()) {
                        var value = world.fuelValues().burnDuration(stack);
                        if (value > 0) {
                            var remainder = stack.getRecipeRemainder();
                            stack.shrink(1);
                            self.fuelTicks = value * 10;
                            self.fuelInitial = self.fuelTicks;
                            if (stack.isEmpty()) {
                                self.setItem(i, ItemStack.EMPTY);
                            }

                            if (!remainder.isEmpty()) {
                                FactoryUtil.tryInsertingRegular(self, remainder);
                                if (!remainder.isEmpty()) {
                                    Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, remainder);
                                }
                            }

                            self.setChanged();
                            return;
                        }
                    }
                }
            }

            if (self.state != 0) {
                self.state = (float) Math.max(self.state - 0.02, 0);
                self.setChanged();
            }
            if (state.getValue(SteamEngineBlock.LIT)) {
                world.setBlockAndUpdate(pos, state.setValue(SteamEngineBlock.LIT, false));
            }
        }
    }

    public void setPowered(int id, boolean b) {
        this.redstoneState = b ? this.redstoneState | (1 << id) : this.redstoneState & ~(1 << id);
    }


    public RedstoneActivationType getRedstoneActivationType() {
        return this.activationType;
    }

    public void setRedstoneActivationType(RedstoneActivationType activationType) {
        this.activationType = activationType;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.items);
        view.putInt("FuelTicks", this.fuelTicks);
        view.putInt("FuelInitial", this.fuelInitial);
        view.putFloat("State", this.state);
        view.store("redstone_activation", RedstoneActivationType.CODEC, this.activationType);
        view.putInt("redstone_state", this.redstoneState);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, items);
        this.fuelInitial = view.getIntOr("FuelInitial", 0);
        this.fuelTicks = view.getIntOr("FuelTicks", 0);
        this.state = view.getFloatOr("State", 0);
        this.activationType = view.read("redstone_activation", RedstoneActivationType.CODEC).orElse(RedstoneActivationType.ALWAYS);
        this.redstoneState = view.getIntOr("redstone_state", 0);
        super.loadAdditional(view);
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel serverWorld, BlockPos pos) {
        if (this.state > 0) {
            modifier.provide(30 * this.state, 110 * this.state, state.getValue(SteamEngineBlock.FACING).getAxisDirection());
        }
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.items;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.level != null && this.level.fuelValues().isFuel(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    public void createGui(ServerPlayer player) {
        new Gui(player);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level == null) {
            return;
        }

        var block = (SteamEngineBlock) this.getBlockState().getBlock();
        for (var pos : BlockPos.betweenClosed(this.getBlockPos().offset(0, -1, 0),
                this.getBlockPos().offset(block.getMaxX(this.getBlockState()), 1, block.getMaxZ(this.getBlockState())))) {
            if (pos.equals(this.getBlockPos())) {
                continue;
            }
            this.level.updateNeighbourForOutputSignal(pos, this.getBlockState().getBlock());
        }
    }

    private class Gui extends SimpleGui {
        private boolean active;

        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x2, player, false);
            this.setTitle(GuiTextures.STEAM_ENGINE.apply(SteamEngineBlockEntity.this.getBlockState().getBlock().getName()));
            this.setSlotRedirect(9 + 3, new FuelSlot(SteamEngineBlockEntity.this, 0, player.level().fuelValues()));
            this.setSlotRedirect(9 + 4, new FuelSlot(SteamEngineBlockEntity.this, 1, player.level().fuelValues()));
            this.setSlotRedirect(9 + 5, new FuelSlot(SteamEngineBlockEntity.this, 2, player.level().fuelValues()));
            this.setSlot(4, GuiTextures.FLAME.getCeil(progress()));
            this.setSlot(7 + 9, GuiUtils.createIteratingButton(
                    SteamEngineBlockEntity.this::getRedstoneActivationType,
                    SteamEngineBlockEntity.this::setRedstoneActivationType,
                    RedstoneActivationType.values(),
                    RedstoneActivationType::createButton
            ));
            this.active = SteamEngineBlockEntity.this.fuelTicks > 0;
            this.open();
        }

        private float progress() {
            return SteamEngineBlockEntity.this.fuelInitial > 0
                    ? Mth.clamp(SteamEngineBlockEntity.this.fuelTicks / (float) SteamEngineBlockEntity.this.fuelInitial, 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(SteamEngineBlockEntity.this.worldPosition)) > (18 * 18)) {
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
