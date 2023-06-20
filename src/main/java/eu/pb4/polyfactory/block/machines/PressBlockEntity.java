package eu.pb4.polyfactory.block.machines;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.PressRecipe;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polyfactory.util.movingitem.InventoryContainerHolderProvider;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PressBlockEntity extends BlockEntity implements InventoryContainerHolderProvider, SidedInventory {
    public static final int INPUT_SLOT = 0;
    public static final int TEMPLATE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] TEMPLATE_SLOTS = {TEMPLATE_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};
    protected double process = 0;
    @Nullable
    protected PressRecipe currentRecipe = null;
    @Nullable
    protected Item currentItem = null;
    private final SimpleContainer[] containers = new SimpleContainer[]{
            new SimpleContainer((x, b) -> this.addMoving(0, x, b), this::removeMoving),
            new SimpleContainer(),
            new SimpleContainer((x, b) -> this.addMoving(2, x, b), this::removeMoving)
    };
    protected int currentItemCount = -1;
    private PressBlock.Model model;
    private boolean active;

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

        if (self.currentRecipe == null && self.currentItem != null && stack.getContainer().get().isOf(self.currentItem) && stack.getContainer().get().getCount() == self.currentItemCount) {
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
            self.currentItem = stack.getContainer().get().getItem();
            self.currentItemCount = stack.getContainer().get().getCount();
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
                stack.getContainer().get().decrement(self.currentRecipe.inputCount());
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

    public double getStress() {
        if (this.active) {
            return this.currentRecipe != null ? this.currentRecipe.minimumSpeed() * 0.8 : 4;
        }
        return 0;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("Items", SimpleContainer.writeArray(containers));
        nbt.putDouble("Progress", this.process);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        SimpleContainer.readArray(containers, nbt.getList("Items", NbtElement.INT_TYPE));
        this.process = nbt.getDouble("Progress");
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(PressBlock.INPUT_FACING);
        return facing == side ? INPUT_SLOTS : (facing.getOpposite() == side ? OUTPUT_SLOTS : new int[0]);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == INPUT_SLOT && (dir == null || this.getCachedState().get(PressBlock.INPUT_FACING) == dir);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        var facing = this.getCachedState().get(PressBlock.INPUT_FACING);
        return (slot == INPUT_SLOT && facing == dir) || (slot != INPUT_SLOT && facing.getOpposite() == dir);
    }

    public void openGui(ServerPlayerEntity player) {
        new Gui(player);
    }


    private void updatePosition(int id) {
        var c = containers[id];

        if (!c.isContainerEmpty()) {
            var base = Vec3d.ofCenter(this.pos).add(0, 0.4, 0);

            if (id == 2) {
                base = base.offset(this.getCachedState().get(PressBlock.INPUT_FACING), -0.3);
            }

            c.getContainer().setPos(base);
        }
    }

    private void addMoving(int i, MovingItem x, boolean newlyAdded) {
        if (this.model != null) {
            if (newlyAdded) {
                updatePosition(i);
                this.model.addElement(x);
            } else {
                this.model.addElementWithoutUpdates(x);
                updatePosition(i);
            }
        }
        this.markDirty();
    }

    private void removeMoving(MovingItem movingItem, boolean fullRemove) {
        if (fullRemove) {
            this.model.removeElement(movingItem);
        } else {
            this.model.removeElementWithoutUpdates(movingItem);
        }
        this.markDirty();
    }

    @Override
    public ContainerHolder getContainerHolder(int slot) {
        return containers[slot];
    }

    @Override
    public int size() {
        return containers.length;
    }

    @Override
    public boolean isEmpty() {
        return containers[0].isContainerEmpty() && containers[1].isContainerEmpty() && containers[2].isContainerEmpty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        for (var x : containers) {
            x.clearContainer();
        }
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_3X3, player, false);
            this.setTitle(PressBlockEntity.this.getCachedState().getBlock().getName());
            var x = new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).setName(Text.empty());
            this.setSlot(1, x);
            this.setSlotRedirect(0, new Slot(PressBlockEntity.this, INPUT_SLOT, 0, 0));
            this.setSlotRedirect(2, new Slot(PressBlockEntity.this, TEMPLATE_SLOT, 0, 0));
            this.setSlot(3, x);
            this.setSlot(4, new GuiElementInterface() {
                @Override
                public ItemStack getItemStack() {
                    return new GuiElementBuilder(Items.GOLDEN_PICKAXE)
                            .setName(Text.empty())
                            .hideFlags()
                            .setDamage(PressBlockEntity.this.currentRecipe != null
                                    ? Items.GOLDEN_PICKAXE.getMaxDamage() - (int) (MathHelper.clamp(PressBlockEntity.this.process, 0, 1) * Items.GOLDEN_PICKAXE.getMaxDamage())
                                    : Items.GOLDEN_PICKAXE.getMaxDamage()
                            ).asStack();
                }
            });
            this.setSlot(5, x);
            this.setSlotRedirect(7, new FurnaceOutputSlot(player, PressBlockEntity.this, OUTPUT_SLOT, 1, 0));
            this.setSlot(6, x);
            this.setSlot(8, x);

            this.open();
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(PressBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }


}
