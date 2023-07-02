package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.MixingRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.movingitem.InventorySimpleContainerProvider;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MixerBlockEntity extends TallItemMachineBlockEntity {
    public static final int OUTPUT_SLOT = 6;
    public static final int INPUT_FIRST = 0;
    private static final int[] OUTPUT_SLOTS = { OUTPUT_SLOT };
    private static final int[] INPUT_SLOTS = { 0, 1, 2, 3, 4, 5 };
    protected double process = 0;
    @Nullable
    protected MixingRecipe currentRecipe = null;
    private boolean active;
    private final SimpleContainer[] containers = new SimpleContainer[] {
            new SimpleContainer((x, b) -> this.addMoving(0, x, b), this::removeMoving),
            new SimpleContainer((x, b) -> this.addMoving(1, x, b), this::removeMoving),
            new SimpleContainer((x, b) -> this.addMoving(2, x, b), this::removeMoving),
            new SimpleContainer((x, b) -> this.addMoving(3, x, b), this::removeMoving),
            new SimpleContainer((x, b) -> this.addMoving(4, x, b), this::removeMoving),
            new SimpleContainer((x, b) -> this.addMoving(5, x, b), this::removeMoving),
            new SimpleContainer((x, b) -> this.addMoving(6, x, b), this::removeMoving)
    };
    private MixerBlock.Model model;
    private boolean inventoryChanged = false;

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.MIXER, pos, state);
    }

    protected void updatePosition(int id) {
        var c = containers[id];

        if (!c.isContainerEmpty()) {
            Vec3d base;
            if (id == OUTPUT_SLOT) {
                base = Vec3d.ofCenter(this.pos).add(0, 0.5, 0);
            } else {
                base = Vec3d.ofCenter(this.pos).add(((id >> 1) - 0.5f) * 0.26f, 0.4 - id * 0.005, ((id % 2) - 0.5) * 0.4);
            }

            c.getContainer().setPos(base);
            c.getContainer().scale(0.5f);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        this.writeInventoryNbt(nbt);
        nbt.putDouble("Progress", this.process);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.readInventoryNbt(nbt);
        this.process = nbt.getDouble("Progress");
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(GrinderBlock.INPUT_FACING);
        return facing.getOpposite() == side ? INPUT_SLOTS : (facing == side ? OUTPUT_SLOTS : new int[0]);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot != OUTPUT_SLOT;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT;
    }

    public void openGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (MixerBlockEntity) t;

        if (self.model == null) {
            self.model = (MixerBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            for (int i = 0; i < self.containers.length; i++) {
                self.updatePosition(i);
                self.containers[i].maybeAdd(self.model);
            }
        }

        if (self.isInputEmpty()) {
            self.process = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        if (self.currentRecipe == null && !self.inventoryChanged) {
            self.process = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        if (self.inventoryChanged && (self.currentRecipe == null || !self.currentRecipe.matches(self, world))) {
            self.process = 0;
            self.currentRecipe = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.MIXER, self, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.model.setActive(false);
                self.model.tick();
                self.inventoryChanged = false;
                return;
            }
        }
        self.inventoryChanged = false;
        self.active = true;
        self.model.setActive(true);
        var fullSpeed = RotationUser.getRotation((ServerWorld) world, pos.up()).speed();
        self.model.rotate((float) (fullSpeed * MathHelper.RADIANS_PER_DEGREE * 5));
        self.model.tick();

        if (self.process >= self.currentRecipe.time()) {
            var currentOutput = self.getStack(OUTPUT_SLOT);
            {
                var output = self.currentRecipe.getOutput(world.getRegistryManager());

                if (!currentOutput.isEmpty() && (!ItemStack.canCombine(currentOutput, output) || output.getCount() + currentOutput.getCount() > output.getMaxCount())) {
                    return;
                }
            }
            self.process = 0;

            self.currentRecipe.applyRecipeUse(self, world);
            var output = self.currentRecipe.craft(self, world.getRegistryManager());

            if (currentOutput.isEmpty()) {
                self.setStack(OUTPUT_SLOT, output);
            } else {
                currentOutput.increment(output.getCount());
            }
            self.markDirty();
        } else {
            var d = Math.max(self.currentRecipe.optimalSpeed() - self.currentRecipe.minimumSpeed(), 1);
            var speed = Math.min(Math.max(Math.abs(fullSpeed) - self.currentRecipe.minimumSpeed(), 0), d) / d / 20;
            if (speed > 0) {
                self.process += speed;
                if (self.world != null) {
                    markDirty(self.world, self.pos, self.getCachedState());
                }
            }
        }
    }

    private boolean isInputEmpty() {
        for (int i = 0; i < OUTPUT_SLOT; i++) {
            if (!this.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public double getStress() {
        if (this.active) {
            return this.currentRecipe != null ? this.currentRecipe.optimalSpeed() * 0.6 : 4;
        }
        return 0;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        this.inventoryChanged = true;
    }

    @Override
    public SimpleContainer[] getContainers() {
        return this.containers;
    }

    @Override
    public @Nullable BaseModel getModel() {
        return this.model;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            //this.setTitle(GuiTextures.GRINDER.apply(MixerBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(1, new Slot(MixerBlockEntity.this, 0, 0, 0));
            this.setSlotRedirect(2, new Slot(MixerBlockEntity.this, 1, 1, 0));
            this.setSlotRedirect(1 + 9, new Slot(MixerBlockEntity.this, 2, 2, 0));
            this.setSlotRedirect(2 + 9, new Slot(MixerBlockEntity.this, 3, 3, 0));
            this.setSlotRedirect(1 + 18, new Slot(MixerBlockEntity.this, 4, 4, 0));
            this.setSlotRedirect(2 + 18, new Slot(MixerBlockEntity.this, 5, 5, 0));
            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL.get(progress()));
            this.setSlotRedirect(6 + 9, new FurnaceOutputSlot(player, MixerBlockEntity.this, OUTPUT_SLOT, 3, 0));
            while (this.getFirstEmptySlot() != -1) {
                this.addSlot(Items.WHITE_STAINED_GLASS_PANE.getDefaultStack());
            }
            this.open();
        }

        private float progress() {
            return MixerBlockEntity.this.currentRecipe != null
                    ? (float) MathHelper.clamp(MixerBlockEntity.this.process / MixerBlockEntity.this.currentRecipe.time(), 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(MixerBlockEntity.this.pos)) > (18*18)) {
                this.close();
            }
            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL.get(progress()));
            super.onTick();
        }
    }
}
