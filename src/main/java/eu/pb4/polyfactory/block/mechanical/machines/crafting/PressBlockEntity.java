package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.PressRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

public class PressBlockEntity extends TallItemMachineBlockEntity {
    public static final int INPUT_SLOT = 0;
    public static final int INPUT_2_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] INPUT_2_SLOTS = {INPUT_2_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};
    private final SimpleContainer[] containers = new SimpleContainer[]{
            new SimpleContainer(0, this::addMoving, this::removeMoving),
            new SimpleContainer(),
            new SimpleContainer(2, this::addMoving, this::removeMoving)
    };
    protected double process = 0;
    @Nullable
    protected PressRecipe currentRecipe = null;
    @Nullable
    protected Item currentItem = null;
    protected int currentItemCount = -1;
    private PressBlock.Model model;
    private boolean active;
    private Item currentItem2;
    private int currentItemCount2;

    public PressBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PRESS, pos, state);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (PressBlockEntity) t;

        if (self.model == null) {
            self.model = (PressBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            self.updatePosition(0);
            self.updatePosition(2);
            self.containers[0].maybeAdd(self.model);
            self.containers[2].maybeAdd(self.model);
        }
        var stack = self.containers[0];
        var stack2 = self.containers[1];

        if (self.process < 0 && stack.isContainerEmpty()) {
            var speed = Math.max(Math.abs(RotationUser.getRotation((ServerWorld) world, pos.up()).speed()), 0);

            self.process += speed / 120;
            self.model.updatePiston(self.process);
            self.active = true;
            self.model.tick();
            return;
        }

        if (stack.isContainerEmpty()) {
            if (self.process != 0) {
                self.model.updatePiston(0);
            }
            self.process = 0;
            self.active = false;
            self.model.tick();
            return;
        }

        if (self.currentRecipe == null && self.currentItem != null && stack.getStack().isOf(self.currentItem) && stack.getStack().getCount() == self.currentItemCount
                && stack2.getStack().isOf(self.currentItem2) && stack2.getStack().getCount() == self.currentItemCount2) {
            if (self.process != 0) {
                self.model.updatePiston(0);
            }
            self.process = 0;
            self.model.tick();
            self.active = false;
            return;
        }

        if (self.currentRecipe == null || !self.currentRecipe.matches(self, world)) {
            if (self.process != 0) {
                self.model.updatePiston(0);
            }
            self.process = 0;
            self.currentItem = stack.getStack().getItem();
            self.currentItem2 = stack2.getStack().getItem();
            self.currentItemCount = stack.getStack().getCount();
            self.currentItemCount2 = stack2.getStack().getCount();
            self.currentRecipe = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.PRESS, self, world).orElse(null);

            if (self.currentRecipe == null) {
                self.model.tick();
                self.active = false;
                return;
            }
        }

        self.active = true;
        if (self.process >= 1) {
            if (self.getStack(OUTPUT_SLOT).isEmpty()) {
                self.process = -0.3;
                stack.getStack().decrement(self.currentRecipe.inputA().count());
                stack2.getStack().decrement(self.currentRecipe.inputB().count());
                var out = self.currentRecipe.craft(self, self.world.getRegistryManager());

                self.setStack(OUTPUT_SLOT, out);
            }
        } else {
            var speed = Math.max(Math.abs(RotationUser.getRotation((ServerWorld) world, pos.up(1)).speed()), 0);

            if (speed >= self.currentRecipe.minimumSpeed() && self.getStack(OUTPUT_SLOT).isEmpty()) {
                self.process += speed / 100;
                self.model.updatePiston(self.process);
            }
        }
        self.model.tick();
    }

    @Override
    public @Nullable BaseModel getModel() {
        return this.model;
    }

    public double getStress() {
        if (this.active) {
            return this.currentRecipe != null ? this.currentRecipe.minimumSpeed() * 0.8 : 4;
        }
        return 0;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);
        if (slot == INPUT_2_SLOT && this.model != null) {
            this.model.setItem(stack.copyWithCount(1));
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
        var facing = this.getCachedState().get(PressBlock.INPUT_FACING);
        if (facing == side) {
            return INPUT_SLOTS;
        } else if (facing.getOpposite() == side) {
            return OUTPUT_SLOTS;
        } else if (facing.rotateYClockwise().getAxis() == side.getAxis()) {
            return INPUT_2_SLOTS;
        }

        return new int[0];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return (slot == INPUT_SLOT && (dir == null || this.getCachedState().get(PressBlock.INPUT_FACING) == dir))
                || (slot == INPUT_2_SLOT && dir == null || this.getCachedState().get(PressBlock.INPUT_FACING).rotateYClockwise().getAxis() == dir.getAxis());
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        var facing = this.getCachedState().get(PressBlock.INPUT_FACING);
        return (slot == INPUT_SLOT && facing == dir) || (slot != INPUT_SLOT && facing.getOpposite() == dir);
    }

    public void openGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    protected void updatePosition(int id) {
        var c = containers[id];

        if (!c.isContainerEmpty()) {
            var base = Vec3d.ofCenter(this.pos).add(0, 0.4, 0);

            if (id == 2) {
                base = base.offset(this.getCachedState().get(PressBlock.INPUT_FACING), -0.3);
            }

            c.getContainer().setPos(base);
            c.getContainer().scale(1);
        }
    }

    @Override
    public SimpleContainer[] getContainers() {
        return this.containers;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.setTitle(GuiTextures.PRESS.apply(PressBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(3, new Slot(PressBlockEntity.this, INPUT_SLOT, 0, 0));
            this.setSlotRedirect(5, new Slot(PressBlockEntity.this, INPUT_2_SLOT, 0, 0));
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            this.setSlotRedirect(22, new FurnaceOutputSlot(player, PressBlockEntity.this, OUTPUT_SLOT, 1, 0));

            this.open();
        }

        private float progress() {
            return (float) MathHelper.clamp(PressBlockEntity.this.process, 0, 1);
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(PressBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            super.onTick();
        }
    }


}
