package eu.pb4.polyfactory.block.mechanical.source;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.ui.FuelSlot;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceFuelSlot;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SteamEngineBlockEntity extends BlockEntity implements MinimalSidedInventory {
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
            self.fuelTicks--;
            self.state = (float) Math.min(self.state + 0.005, 1);

            if (!state.get(SteamEngineBlock.LIT)) {
                world.setBlockState(pos, state.with(SteamEngineBlock.LIT, true));
            }
            self.markDirty();
        } else {
            for (int i = 0; i < 3; i++) {
                var stack = self.getStack(i);

                if (!stack.isEmpty()) {
                    var value = FuelRegistry.INSTANCE.get(stack.getItem());
                    if (value != null) {
                        stack.decrement(1);
                        self.fuelTicks = value;
                        self.fuelInitial = value;
                        if (self.isEmpty()) {
                            self.setStack(i, ItemStack.EMPTY);
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
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, this.items);
        nbt.putInt("FuelTicks", this.fuelTicks);
        nbt.putInt("FuelInitial", this.fuelInitial);
        nbt.putFloat("State", this.state);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, items);
        this.fuelInitial = nbt.getInt("FuelInitial");
        this.fuelTicks = nbt.getInt("FuelTicks");
        this.state = nbt.getFloat("State");
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld serverWorld, BlockPos pos) {
        if (this.state > 0) {
            modifier.provide(20 * this.state, 100 * this.state);
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
        return AbstractFurnaceBlockEntity.canUseAsFuel(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    public void openGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X2, player, false);
            this.setTitle(SteamEngineBlockEntity.this.getCachedState().getBlock().getName());
            this.setSlotRedirect(9 + 3, new FuelSlot(SteamEngineBlockEntity.this, 0, 0, 0));
            this.setSlotRedirect(9 + 4, new FuelSlot(SteamEngineBlockEntity.this, 1, 1, 0));
            this.setSlotRedirect(9 + 5, new FuelSlot(SteamEngineBlockEntity.this, 2, 2, 0));
            this.setSlot(4, GuiTextures.FLAME.get(progress()));
            while (this.getFirstEmptySlot() != -1) {
                this.addSlot(Items.WHITE_STAINED_GLASS_PANE.getDefaultStack());
            }
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
            this.setSlot(4, GuiTextures.FLAME.get(progress()));
            super.onTick();
        }
    }
}
