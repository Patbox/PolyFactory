package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.fluid.DrainRecipe;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.TagLimitedSlot;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;

public class MDrainBlockEntity extends TallItemMachineBlockEntity implements FluidInputOutput.ContainerBased {

    public static final int CATALYST_FIRST = 2;
    public static final int OUTPUT_FIRST = 1;
    public static final int INPUT_FIRST = 0;
    public static final long FLUID_CAPACITY = FluidType.BLOCK_AMOUNT;
    private static final int[] OUTPUT_SLOTS = {1};
    private static final int[] INPUT_SLOTS = {0};
    private final SimpleContainer[] containers = new SimpleContainer[]{
            new SimpleContainer(0, this::addMoving, this::removeMoving),
            new SimpleContainer(1, this::addMoving, this::removeMoving),
            new SimpleContainer()
    };
    protected double process = 0;
    protected double speedScale = 0;
    @Nullable
    protected RecipeEntry<DrainRecipe> currentRecipe = null;
    private boolean active;
    private MDrainBlock.Model model;
    private boolean inventoryChanged = false;
    private final FluidContainer fluidContainer = new FluidContainer(FLUID_CAPACITY, this::markDirty);
    public MDrainBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.MECHANICAL_DRAIN, pos, state);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (MDrainBlockEntity) t;

        if (self.model == null) {
            self.model = (MDrainBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            for (int i = 0; i < CATALYST_FIRST; i++) {
                self.updatePosition(i);
                self.containers[i].maybeAdd(self.model);
            }
        }
        self.state = null;
        var temp = BlockHeat.getReceived(world, pos);

        self.fluidContainer.tick((ServerWorld) world, pos, temp, self::addToOutputOrDrop);
        self.model.setFluid(self.fluidContainer.topFluid(), self.fluidContainer.getFilledPercentage());

        if (self.isInputEmpty()) {
            self.process = 0;
            self.updateInputPosition();
            self.speedScale = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        if (self.currentRecipe == null && !self.inventoryChanged) {
            self.process = 0;
            self.updateInputPosition();
            self.speedScale = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            self.state = INCORRECT_ITEMS_TEXT;
            return;
        }
        var inputStack = self.getStack(INPUT_FIRST);

        var input = DrainInput.of(inputStack.copy(), self.catalyst(), self.fluidContainer, false);

        if (self.currentRecipe == null || !self.currentRecipe.value().matches(input, world)) {
            self.process = 0;
            self.updateInputPosition();
            self.speedScale = 0;
            self.currentRecipe = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.DRAIN, input, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.model.setActive(false);
                self.model.tick();
                self.inventoryChanged = false;
                self.state = INCORRECT_ITEMS_TEXT;
                return;
            }
        }
        self.inventoryChanged = false;


        self.active = true;
        self.model.setActive(true);
        var rot = RotationUser.getRotation(world, pos.up());
        var fullSpeed = rot.speed();
        self.model.rotate((float) fullSpeed);
        self.model.tick();

        if (self.process >= 1) {
            var itemOut = self.currentRecipe.value().craft(input, world.getRegistryManager());
            var currentOutput = self.getStack(OUTPUT_FIRST);
            if (currentOutput.isEmpty()) {
                self.setStack(OUTPUT_FIRST, itemOut);
            } else if (ItemStack.areItemsAndComponentsEqual(itemOut, currentOutput) && currentOutput.getCount() + itemOut.getCount() < itemOut.getMaxCount()) {
                currentOutput.increment(itemOut.getCount());
            } else {
                return;
            }
            inputStack.decrement(1);
            if (inputStack.isEmpty()) {
                self.setStack(INPUT_FIRST, ItemStack.EMPTY);
            }
            for (var fluid : self.currentRecipe.value().fluidInput(input)) {
                self.fluidContainer.extract(fluid, false);
            }
            for (var fluid : self.currentRecipe.value().fluidOutput(input)) {
                self.fluidContainer.insert(fluid, false);
            }
            world.playSound(null, pos, self.currentRecipe.value().soundEvent().value(), SoundCategory.BLOCKS);
            self.process = 0;
            self.updateInputPosition();
            self.markDirty();
        } else {
            var d = 1;
            var speed = Math.min(Math.max(Math.abs(fullSpeed), 0), d) / 120;
            self.speedScale = speed;
            if (speed > 0) {
                self.process += speed;
                markDirty(world, pos, self.getCachedState());
                var ppos = self.containers[0].getPos();
                var fluid = Util.getRandomOrEmpty(self.currentRecipe.value().fluidOutput(input), world.random);

                if (fluid.isPresent() && ppos != null) {
                    ((ServerWorld) world).spawnParticles(fluid.get().instance().particle(),
                            ppos.x, ppos.y, ppos.z, 0,
                            0, 0, 0, 0);
                }
                self.updateInputPosition();
                return;
            }

            self.state = rot.getStateTextOrElse(TOO_SLOW_TEXT);
        }
    }

    private void updateInputPosition() {
        this.updatePosition(0);
        this.containers[0].tick();
    }

    protected void updatePosition(int id) {
        var c = containers[id];

        if (!c.isContainerEmpty()) {
            assert c.getContainer() != null;
            var container = c.getContainer();
            Vec3d base = Vec3d.ofCenter(this.pos);
            Quaternionf rot;
            var dir = this.getCachedState().get(MDrainBlock.INPUT_FACING);
            if (id == INPUT_FIRST) {
                base = base.add(0, 0.50, 0).offset(dir, -(this.process - 0.5) * (8 / 16f)).offset(dir.rotateYClockwise(), -4 / 16f);
                rot = Direction.UP.getRotationQuaternion()
                        .rotateY(-dir.rotateYClockwise().asRotation() * MathHelper.RADIANS_PER_DEGREE)
                        .rotateX(MathHelper.PI * 3 / 4);
            } else {
                base = base.add(0, 0.45, 0).offset(dir, -0.3);
                rot = dir.getOpposite().getRotationQuaternion();
            }

            container.setPos(base);
            container.scale(0.75f);
            container.setRotation(rot);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        this.writeInventoryNbt(nbt, lookup);
        nbt.putDouble("Progress", this.process);
        nbt.put("fluid", this.fluidContainer.toNbt(lookup));
        super.writeNbt(nbt, lookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        this.readInventoryNbt(nbt, lookup);
        this.process = nbt.getDouble("Progress");
        this.fluidContainer.fromNbt(lookup, nbt, "fluid");
        super.readNbt(nbt, lookup);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        var f = components.get(FactoryDataComponents.FLUID);
        if (f != null) {
            this.fluidContainer.clear();
            f.extractTo(this.fluidContainer);
        }
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
        componentMapBuilder.add(FactoryDataComponents.FLUID, FluidComponent.copyFrom(this.fluidContainer));
    }

    @Override
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        nbt.remove("fluid");
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(MDrainBlock.INPUT_FACING);
        return facing.getOpposite() == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == OUTPUT_FIRST;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_FIRST;
    }

    public void createGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        super.setStack(slot, stack);
        if (slot == CATALYST_FIRST) {
            this.model.setCatalyst(stack);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var stack = player.getStackInHand(Hand.MAIN_HAND);
        if ((stack.isEmpty() || (ItemStack.areItemsAndComponentsEqual(stack, this.catalyst()) && stack.getCount() < stack.getMaxCount()))
                && hit.getSide() == Direction.UP && !this.catalyst().isEmpty()) {
            if (stack.isEmpty()) {
                player.setStackInHand(Hand.MAIN_HAND, this.catalyst());
            } else {
                stack.increment(1);
            }
            this.setCatalyst(ItemStack.EMPTY);
            return ActionResult.SUCCESS;
        } else if (stack.isIn(FactoryItemTags.DRAIN_CATALYST) && hit.getSide() == Direction.UP && this.catalyst().isEmpty()) {
            this.setCatalyst(stack.copyWithCount(1));
            stack.decrementUnlessCreative(1, player);
            return ActionResult.SUCCESS;
        }

        var container = this.getFluidContainer();
        var copy = stack.copy();
        var input = DrainInput.of(copy, this.catalyst(), container, !(player instanceof FakePlayer));
        var optional = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.DRAIN, input, world);
        if (optional.isEmpty()) {
            return super.onUse(state, world, pos, player, hit);
        }
        var recipe = optional.get().value();
        var itemOut = recipe.craft(input, player.getRegistryManager());
        for (var fluid : recipe.fluidInput(input)) {
            container.extract(fluid, false);
        }
        player.setStackInHand(Hand.MAIN_HAND, ItemUsage.exchangeStack(stack, player, itemOut));
        for (var fluid : recipe.fluidOutput(input)) {
            container.insert(fluid, false);
        }
        world.playSound(null, pos, recipe.soundEvent().value(), SoundCategory.BLOCKS);
        return ActionResult.SUCCESS;
    }

    private void addToOutputOrDrop(ItemStack stack) {
        FactoryUtil.insertBetween(this, OUTPUT_FIRST, this.size(), stack);
        if (!stack.isEmpty()) {
            assert this.world != null;
            ItemScatterer.spawn(this.world, this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, stack);
        }
    }

    private boolean isInputEmpty() {
        for (int i = 0; i < OUTPUT_FIRST; i++) {
            if (!this.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public double getStress() {
        if (this.active) {
            return 1;
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
    public @Nullable BlockModel getModel() {
        return this.model;
    }

    public FluidContainer getFluidContainer() {
        return this.fluidContainer;
    }

    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.fluidContainer;
    }


    public ItemStack catalyst() {
        return this.getStack(CATALYST_FIRST);
    }

    public void setCatalyst(ItemStack catalyst) {
        this.setStack(CATALYST_FIRST, catalyst);
        if (this.model != null) {
            this.model.setCatalyst(catalyst);
        }
        this.markDirty();
    }

    private class Gui extends SimpleGui {
        private int lastFluidUpdate = -1;
        private int delayTick = -1;

        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.updateTitleAndFluid();
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.DRAIN));
            var fluidSlot = fluidContainer.guiElement(true);


            //noinspection PointlessArithmeticExpression
            this.setSlot(6 + 0 * 9, fluidSlot);
            //noinspection PointlessArithmeticExpression
            this.setSlot(6 + 1 * 9, fluidSlot);
            this.setSlot(6 + 2 * 9, fluidSlot);

            this.setSlotRedirect(2, new Slot(MDrainBlockEntity.this, 0, 0, 0));
            this.setSlotRedirect(9 + 5, new FurnaceOutputSlot(player,MDrainBlockEntity.this, 1, 1, 0));
            this.setSlotRedirect(9 * 2 + 2, new TagLimitedSlot(MDrainBlockEntity.this, 2, FactoryItemTags.DRAIN_CATALYST) {
                @Override
                public int getMaxItemCount() {
                    return 1;
                }

                @Override
                public int getMaxItemCount(ItemStack stack) {
                    return 1;
                }
            });
            this.setSlot(9 + 3, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            this.open();
        }

        private void updateTitleAndFluid() {
            var text = GuiTextures.MECHANICAL_DRAIN.apply(
                    Text.empty()
                            .append(Text.literal(GuiTextures.MECHANICAL_DRAIN_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.MIXER.render(MDrainBlockEntity.this.fluidContainer::provideRender))
                            .append(Text.literal(GuiTextures.MECHANICAL_DRAIN_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(MDrainBlockEntity.this.getCachedState().getBlock().getName())
            );


            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }


            this.lastFluidUpdate = MDrainBlockEntity.this.fluidContainer.updateId();
        }

        private float progress() {
            return MDrainBlockEntity.this.currentRecipe != null
                    ? (float) MathHelper.clamp(MDrainBlockEntity.this.process, 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(MDrainBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            if (MDrainBlockEntity.this.fluidContainer.updateId() != this.lastFluidUpdate && delayTick < 0) {
                delayTick = 3;
            }
            if (this.delayTick-- == 0) {
                this.updateTitleAndFluid();
            }

            this.setSlot(3 + 9, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            super.onTick();
        }
    }
}
