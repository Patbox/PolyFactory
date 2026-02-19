package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.DrainBlockEntity;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.block.other.ItemOutputBufferBlock;
import eu.pb4.polyfactory.block.other.OutputContainerOwner;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.PressInput;
import eu.pb4.polyfactory.recipe.press.PressRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SubContainer;
import eu.pb4.polyfactory.util.movingitem.SimpleMovingItemContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PressBlockEntity extends TallItemMachineBlockEntity implements OutputContainerOwner {
    public static final int INPUT_SLOT = 0;
    public static final int INPUT_2_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] INPUT_2_SLOTS = {INPUT_2_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};
    private final SimpleMovingItemContainer[] containers = new SimpleMovingItemContainer[]{
            new SimpleMovingItemContainer(0, this::addMoving, this::removeMoving),
            new SimpleMovingItemContainer(),
            new SimpleMovingItemContainer(2, this::addMoving, this::removeMoving)
    };
    private final Container outputContainer = new SubContainer(this, OUTPUT_SLOT);
    protected double process = 0;
    @Nullable
    protected RecipeHolder<PressRecipe> currentRecipe = null;
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

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
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
            var speed = Math.max(Math.abs(RotationUser.getRotation(world, pos.above()).speed()), 0);

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

        if (self.currentRecipe == null && self.currentItem != null && stack.getStack().is(self.currentItem) && stack.getStack().getCount() == self.currentItemCount
                && stack2.getStack().is(self.currentItem2) && stack2.getStack().getCount() == self.currentItemCount2) {
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
            self.currentRecipe = ((ServerLevel) world).recipeAccess().getRecipeFor(FactoryRecipeTypes.PRESS, input, world).orElse(null);

            if (self.currentRecipe == null) {
                self.model.tick();
                self.active = false;
                self.state = INCORRECT_ITEMS_TEXT;
                return;
            }
        }

        self.active = true;

        if (self.process >= 1 || self.delayedOutput != null) {
            var nextOut = self.delayedOutput != null ? self.delayedOutput : self.currentRecipe.value().assemble(input, self.level.registryAccess());

            boolean success = true;

            var outputContainer = self.getOutputContainer();

            {
                var items = new ArrayList<ItemStack>();
                items.add(nextOut.copy());

                var inv = new net.minecraft.world.SimpleContainer(outputContainer.getContainerSize());
                for (int i = 0; i < outputContainer.getContainerSize(); i++) {
                    inv.setItem(i, outputContainer.getItem(i).copy());
                }

                for (var item : items) {
                    FactoryUtil.tryInsertingInv(inv, item, null);

                    if (!item.isEmpty()) {
                        self.state = OUTPUT_FULL_TEXT;
                        success = false;
                    }
                }
            }

            var fluids = self.currentRecipe.value().outputFluids(input);
            var drains = new ArrayList<Pair<DrainBlockEntity, Direction>>();
            var totalFluidAllowed = 0L;

            if (!fluids.isEmpty()) {
                for (var dir : Direction.Plane.HORIZONTAL) {
                    if (world.getBlockEntity(pos.relative(dir)) instanceof DrainBlockEntity be) {
                        drains.add(new Pair<>(be, dir.getOpposite()));
                        var c = be.getFluidContainer(dir.getOpposite());
                        if (c != null) {
                            totalFluidAllowed += c.empty();
                        }
                    }
                }
            }

            if (self.currentRecipe.value().fluidsRequired()) {
                if (fluids.isEmpty()) {
                    self.state = INCORRECT_ITEMS_TEXT;
                    success = false;
                } else {
                    var fluidTotal = 0l;
                    for (var x : fluids) {
                        fluidTotal += x.amount();
                    }

                    if (totalFluidAllowed < fluidTotal) {
                        self.state = OUTPUT_FULL_TEXT;
                        success = false;
                    }
                }
            }

            if (success) {
                FactoryUtil.tryInsertingRegular(outputContainer, nextOut);

                if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayer player) {
                    CriteriaTriggers.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), List.of(stack.getStack(), stack2.getStack()));
                }

                if (!fluids.isEmpty()) {
                    var copy = new ArrayList<>(fluids);

                    for (var be : drains) {
                        for (int i = 0; i < copy.size(); i++) {
                            var fluid = copy.get(i);
                            if (fluid.isEmpty()) {
                                continue;
                            }
                            var leftover = be.getFirst().insertFluid(fluid.instance(), fluid.amount(), be.getSecond());
                            copy.set(i, fluid.withAmount(leftover));
                        }
                    }

                    for (var fluid : copy) {
                        if (fluid.isEmpty()) {
                            continue;
                        }
                        for (int i = 0; i < 5; i++) {
                            ((ServerLevel) world).sendParticles(fluid.instance().particle(),
                                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0,
                                    (Math.random() - 0.5) * 0.2, 0.01, (Math.random() - 0.5) * 0.2, 0.3);
                        }
                    }

                    for (var fluid : fluids) {
                        ((ServerLevel) world).sendParticles(fluid.instance().particle(),
                                pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0,
                                (Math.random() - 0.5) * 0.2, 0, (Math.random() - 0.5) * 0.2, 0.2);
                    }
                }

                self.process = -0.6;
                self.model.updatePiston(self.process);
                ((ServerLevel) world).sendParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0,
                        (Math.random() - 0.5) * 0.2, 0, (Math.random() - 0.5) * 0.2, 0.2);
                self.currentRecipe.value().applyRecipeUse(self, world);
                self.delayedOutput = null;
                self.playedSound = false;
            } else {
                self.delayedOutput = nextOut;
            }
        } else {
            var rot = RotationUser.getRotation(world, pos.above(1));
            var speed = Math.max(Math.abs(rot.speed()), 0);

            if (speed >= self.currentRecipe.value().minimumSpeed()) {
                self.process += speed / 100;
                self.model.updatePiston(self.process);

                if (self.process >= 0.4 && !self.playedSound) {
                    world.playSound(null, pos, FactorySoundEvents.BLOCK_PRESS_CRAFT, SoundSource.BLOCKS, 0.1f, 1.2f);
                    self.playedSound = true;
                }
            } else if (world.getGameTime() % 5 == 0) {
                ((ServerLevel) world).sendParticles(ParticleTypes.SMOKE,
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
    public Container getOwnOutputContainer() {
        return this.outputContainer;
    }

    @Override
    public Container getOutputContainer() {
        return ItemOutputBufferBlock.getOutputContainer(this.outputContainer, this.level, this.getBlockPos(), this.getBlockState().getValue(PressBlock.INPUT_FACING).getOpposite());
    }

    @Override
    public boolean isOutputConnectedTo(Direction dir) {
        return this.getBlockState().getValue(PressBlock.INPUT_FACING).getOpposite() == dir;
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
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == INPUT_2_SLOT && this.model != null) {
            this.model.setItem(stack.copyWithCount(1));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        this.writeInventoryView(view);
        view.putDouble("Progress", this.process);
        if (this.delayedOutput != null && !this.delayedOutput.isEmpty()) {
            view.store("DelayedOutput", ItemStack.OPTIONAL_CODEC, this.delayedOutput);
        }
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.readInventoryView(view);
        this.process = view.getDoubleOr("Progress", 0);
        this.delayedOutput = view.read("DelayedOutput", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        if (this.delayedOutput.isEmpty()) {
            this.delayedOutput = null;
        }

        this.currentItem = null;
        this.currentItem2 = null;
        super.loadAdditional(view);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        var facing = this.getBlockState().getValue(PressBlock.INPUT_FACING);
        if (facing == side) {
            return INPUT_SLOTS;
        } else if (facing.getOpposite() == side || side == Direction.DOWN) {
            return OUTPUT_SLOTS;
        } else if (facing.getClockWise().getAxis() == side.getAxis()) {
            return INPUT_2_SLOTS;
        }

        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return (slot == INPUT_SLOT && (dir == null || this.getBlockState().getValue(PressBlock.INPUT_FACING) == dir))
                || (slot == INPUT_2_SLOT && dir == null || this.getBlockState().getValue(PressBlock.INPUT_FACING).getClockWise().getAxis() == dir.getAxis());
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        var facing = this.getBlockState().getValue(PressBlock.INPUT_FACING);
        return (slot == INPUT_SLOT && facing == dir) || (slot != INPUT_SLOT && (facing.getOpposite() == dir || dir == Direction.DOWN));
    }

    public void createGui(ServerPlayer player) {
        new Gui(player);
    }

    protected void updatePosition(int id) {
        var c = containers[id];

        if (!c.isContainerEmpty()) {
            var base = Vec3.atCenterOf(this.worldPosition).add(0, 0.4, 0);

            if (id == 2) {
                base = base.relative(this.getBlockState().getValue(PressBlock.INPUT_FACING), -0.3);
            }

            c.getContainer().setPos(base);
            c.getContainer().scale(1);
        }
    }

    @Override
    public SimpleMovingItemContainer[] getContainers() {
        return this.containers;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.setTitle(GuiTextures.PRESS.apply(PressBlockEntity.this.getBlockState().getBlock().getName()));
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.PRESS));
            this.setSlotRedirect(3, new Slot(PressBlockEntity.this, INPUT_SLOT, 0, 0));
            this.setSlotRedirect(5, new Slot(PressBlockEntity.this, INPUT_2_SLOT, 0, 0));
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            this.setSlotRedirect(22, new FurnaceResultSlot(player, PressBlockEntity.this, OUTPUT_SLOT, 1, 0));

            this.open();
        }

        private float progress() {
            return (float) Mth.clamp(PressBlockEntity.this.process, 0, 1);
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(PressBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            super.onTick();
        }
    }


}
