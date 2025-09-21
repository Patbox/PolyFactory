package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.DrainBlockEntity;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.PressInput;
import eu.pb4.polyfactory.recipe.press.PressRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    protected RecipeEntry<PressRecipe> currentRecipe = null;
    @Nullable
    protected Item currentItem = null;
    protected int currentItemCount = -1;
    private PressBlock.Model model;
    private boolean active;
    private Item currentItem2;
    private int currentItemCount2;
    @Nullable
    private ItemStack delayedOutput;
    private boolean playedSound;

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
        self.state = null;
        if (self.process < 0 && stack.isContainerEmpty()) {
            var speed = Math.max(Math.abs(RotationUser.getRotation((ServerWorld) world, pos.up()).speed()), 0);

            self.process += speed / 120;
            self.model.updatePiston(self.process);
            self.active = true;
            self.delayedOutput = null;
            self.model.tick();
            return;
        }

        if (stack.isContainerEmpty()) {
            if (self.process != 0) {
                self.model.updatePiston(0);
            }
            self.process = 0;
            self.active = false;
            self.delayedOutput = null;
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
            self.delayedOutput = null;
            self.active = false;
            self.state = INCORRECT_ITEMS_TEXT;
            return;
        }

        var input = new PressInput(stack.getStack(), stack2.getStack());

        if (self.currentRecipe == null || !self.currentRecipe.value().matches(input, world)) {
            if (self.process != 0) {
                self.model.updatePiston(0);
            }
            self.process = 0;
            self.delayedOutput = null;
            self.currentItem = stack.getStack().getItem();
            self.currentItem2 = stack2.getStack().getItem();
            self.currentItemCount = stack.getStack().getCount();
            self.currentItemCount2 = stack2.getStack().getCount();
            self.currentRecipe = ((ServerWorld) world).getRecipeManager().getFirstMatch(FactoryRecipeTypes.PRESS, input, world).orElse(null);

            if (self.currentRecipe == null) {
                self.model.tick();
                self.active = false;
                self.state = INCORRECT_ITEMS_TEXT;
                return;
            }
        }

        self.active = true;

        if (self.process >= 1 || self.delayedOutput != null) {
            var nextOut = self.delayedOutput != null ? self.delayedOutput : self.currentRecipe.value().craft(input, self.world.getRegistryManager());
            var currentOut = self.getStack(OUTPUT_SLOT);

            boolean success = false;

            if (currentOut.isEmpty()) {
                success = true;
                self.setStack(OUTPUT_SLOT, nextOut);
            } else if (ItemStack.areItemsAndComponentsEqual(currentOut, nextOut) && currentOut.getCount() + nextOut.getCount() <= currentOut.getMaxCount()) {
                success = true;
                currentOut.increment(nextOut.getCount());
            }

            if (success) {
                if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayerEntity player) {
                    Criteria.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), List.of(stack.getStack(), stack2.getStack()));
                }

                var fluids = self.currentRecipe.value().outputFluids(input);
                if (!fluids.isEmpty()) {
                    var copy = new ArrayList<>(fluids);

                    for (var dir : Direction.Type.HORIZONTAL) {
                        if (world.getBlockEntity(pos.offset(dir)) instanceof DrainBlockEntity be) {
                            for (int i = 0; i < copy.size(); i++) {
                                var fluid = copy.get(i);
                                if (fluid.isEmpty()) {
                                    continue;
                                }
                                var leftover = be.insertFluid(fluid.instance(), fluid.amount(), dir.getOpposite());
                                copy.set(i, fluid.withAmount(leftover));
                            }
                        }
                    }

                    for (var fluid : copy) {
                        if (fluid.isEmpty()) {
                            continue;
                        }
                        for (int i = 0; i < 5; i++) {
                            ((ServerWorld) world).spawnParticles(fluid.instance().particle(),
                                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0,
                                    (Math.random() - 0.5) * 0.2, 0.01, (Math.random() - 0.5) * 0.2, 0.3);
                        }
                    }

                    for (var fluid : fluids) {
                        ((ServerWorld) world).spawnParticles(fluid.instance().particle(),
                                pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0,
                                (Math.random() - 0.5) * 0.2, 0, (Math.random() - 0.5) * 0.2, 0.2);
                    }
                }

                self.process = -0.6;
                self.model.updatePiston(self.process);
                ((ServerWorld) world).spawnParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0,
                        (Math.random() - 0.5) * 0.2, 0, (Math.random() - 0.5) * 0.2, 0.2);
                self.currentRecipe.value().applyRecipeUse(self, world);
                self.delayedOutput = null;
                self.playedSound = false;
            } else {
                self.delayedOutput = nextOut;
            }
        } else {
            var rot = RotationUser.getRotation((ServerWorld) world, pos.up(1));
            var speed = Math.max(Math.abs(rot.speed()), 0);

            if (speed >= self.currentRecipe.value().minimumSpeed()) {
                self.process += speed / 100;
                self.model.updatePiston(self.process);

                if (self.process >= 0.4 && !self.playedSound) {
                    world.playSound(null, pos, FactorySoundEvents.BLOCK_PRESS_CRAFT, SoundCategory.BLOCKS, 0.1f, 1.2f);
                    self.playedSound = true;
                }
            } else if (world.getTime() % 5 == 0) {
                ((ServerWorld) world).spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 0,
                        (Math.random() - 0.5) * 0.2, 0.04, (Math.random() - 0.5) * 0.2, 0.3);

                self.state = rot.getStateTextOrElse(TOO_SLOW_TEXT);
            } else {
                self.state = rot.getStateTextOrElse(TOO_SLOW_TEXT);
            }
        }
        self.model.tick();
    }

    @Override
    public @Nullable BlockModel getModel() {
        return this.model;
    }

    public double getStress() {
        if (this.active) {
            return this.currentRecipe != null ? this.currentRecipe.value().minimumSpeed() * 0.8 : 4;
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
    protected void writeData(WriteView view) {
        this.writeInventoryView(view);
        view.putDouble("Progress", this.process);
        if (this.delayedOutput != null && !this.delayedOutput.isEmpty()) {
            view.put("DelayedOutput", ItemStack.OPTIONAL_CODEC, this.delayedOutput);
        }
        super.writeData(view);
    }

    @Override
    public void readData(ReadView view) {
        this.readInventoryView(view);
        this.process = view.getDouble("Progress", 0);
        this.delayedOutput = view.read("DelayedOutput", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        if (this.delayedOutput.isEmpty()) {
            this.delayedOutput = null;
        }

        this.currentItem = null;
        this.currentItem2 = null;
        super.readData(view);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(PressBlock.INPUT_FACING);
        if (facing == side) {
            return INPUT_SLOTS;
        } else if (facing.getOpposite() == side || side == Direction.DOWN) {
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
        return (slot == INPUT_SLOT && facing == dir) || (slot != INPUT_SLOT && (facing.getOpposite() == dir || dir == Direction.DOWN));
    }

    public void createGui(ServerPlayerEntity player) {
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
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.PRESS));
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
