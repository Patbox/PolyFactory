package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.FluidInputOutput;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.recipe.mixing.MixingRecipe;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.InventoryList;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MixerBlockEntity extends TallItemMachineBlockEntity implements FluidInputOutput.ContainerBased {

    public static final int OUTPUT_FIRST = 6;
    public static final int INPUT_FIRST = 0;
    public static final int SIZE = 9;
    public static final long FLUID_CAPACITY = FluidType.BLOCK_AMOUNT * 2;
    private static final int[] OUTPUT_SLOTS = { 6, 7, 8 };
    private static final int[] INPUT_SLOTS = { 0, 1, 2, 3, 4, 5 };
    protected double process = 0;
    protected float temperature = 0;
    @Nullable
    protected RecipeEntry<MixingRecipe> currentRecipe = null;
    private boolean active;
    private final SimpleContainer[] containers = SimpleContainer.createArray(9, this::addMoving, this::removeMoving);
    private final List<ItemStack> stacks = new InventoryList(this, INPUT_FIRST, OUTPUT_FIRST);
    private final FluidContainerImpl fluidContainer = new FluidContainerImpl(FLUID_CAPACITY, this::markDirty);
    private MixerBlock.Model model;
    private boolean inventoryChanged = false;
    private double speedScale;

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.MIXER, pos, state);
    }

    protected void updatePosition(int id) {
        var c = containers[id];

        if (!c.isContainerEmpty()) {
            Vec3d base;
            if (id >= OUTPUT_FIRST) {
                id = id - OUTPUT_FIRST;
                base = Vec3d.ofCenter(this.pos).add(((id >> 1) - 0.5f) * 0.12f, - id * 0.005, ((id % 2) - 0.5) * 0.2);
            } else {
                base = Vec3d.ofCenter(this.pos).add(((id >> 1) - 0.5f) * 0.15f, -0.15 - id * 0.005, ((id % 2) - 0.5) * 0.2);
            }

            c.getContainer().setPos(base);
            c.getContainer().scale(0.5f);
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
        var facing = this.getCachedState().get(MixerBlock.INPUT_FACING);
        return facing.getOpposite() == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot < OUTPUT_FIRST;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_FIRST;
    }

    public void createGui(ServerPlayerEntity player) {
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
        self.state = null;
        self.temperature = BlockHeat.getReceived(world, pos) + self.fluidContainer.fluidTemperature();

        FluidContainerUtil.tick(self.fluidContainer, (ServerWorld) world, pos, self.temperature, self::addToOutputOrDrop);
        self.model.setFluid(self.fluidContainer.topFluid(), self.fluidContainer.getFilledPercentage());

        if (self.isInputEmpty() && self.fluidContainer.isEmpty()) {
            self.process = 0;
            self.speedScale = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        if (self.currentRecipe == null && !self.inventoryChanged) {
            self.process = 0;
            self.speedScale = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            self.state = INCORRECT_ITEMS_TEXT;
            return;
        }

        var input = new MixingInput(self.stacks, FluidContainerInput.of(self.fluidContainer), world);

        if (self.inventoryChanged && (self.currentRecipe == null || !self.currentRecipe.value().matches(input, world))) {
            self.process = 0;
            self.speedScale = 0;
            self.currentRecipe = ((ServerWorld) world).getRecipeManager().getFirstMatch(FactoryRecipeTypes.MIXER, input, world).orElse(null);

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


        if (self.temperature < self.currentRecipe.value().minimumTemperature(input) || self.temperature > self.currentRecipe.value().maxTemperature(input)) {
            self.active = false;
            self.state = self.temperature < self.currentRecipe.value().minimumTemperature(input) ? TOO_COLD_TEXT : TOO_HOT_TEXT;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        self.active = true;
        self.model.setActive(true);
        var rot = RotationUser.getRotation((ServerWorld) world, pos.up());
        var fullSpeed = rot.speed();
        self.model.rotate((float) fullSpeed);
        self.model.tick();

        if (self.process >= self.currentRecipe.value().time(input)) {
            var output = self.currentRecipe.value().craft(input, world.getRegistryManager());
            var outFluid = self.currentRecipe.value().fluidOutput(input);

            var emptyFluids = self.fluidContainer.empty();
            for (var x : self.currentRecipe.value().fluidInput(input)) {
                emptyFluids -= x.used();
            }


            for (var f : outFluid) {
                emptyFluids -= f.amount();
            }
            if (emptyFluids < 0) {
                self.state = OUTPUT_FULL_TEXT;
                return;
            }

            {
                var items = new ArrayList<ItemStack>();
                items.add(output.copy());
                for (var x : self.currentRecipe.value().remainders(input)) {
                    items.add(x.copy());
                }

                var inv = new SimpleInventory(3);
                for (int i = 0; i < 3; i++) {
                    inv.setStack(i, self.getStack(OUTPUT_FIRST + i).copy());
                }

                for (var item : items) {
                    FactoryUtil.tryInsertingInv(inv, item, null);

                    if (!item.isEmpty()) {
                        self.state = OUTPUT_FULL_TEXT;
                        return;
                    }
                }
            }

            self.currentRecipe.value().applyRecipeUse(self, world);
            self.process = 0;

            if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayerEntity player) {
                Criteria.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), self.asCraftingRecipeInput().getStacks());
                TriggerCriterion.trigger(player, FactoryTriggers.MIXER_CRAFTS);
            }

            FactoryUtil.insertBetween(self, OUTPUT_FIRST, self.size(), output);
            for (var x : self.currentRecipe.value().remainders(input)) {
                FactoryUtil.insertBetween(self, OUTPUT_FIRST, self.size(), x);
            }

            for (var f : outFluid) {
                self.fluidContainer.insert(f, false);
            }
            self.markDirty();
        } else {
            var d = Math.max(self.currentRecipe.value().optimalSpeed(input) - self.currentRecipe.value().minimumSpeed(input), 1);
            var speed = Math.min(Math.max(Math.abs(fullSpeed) - self.currentRecipe.value().minimumSpeed(input), 0), d) / d / 20;
            self.speedScale = speed;
            if (speed > 0) {
                self.process += speed;
                markDirty(world, pos, self.getCachedState());

                var stack = self.getStack(world.random.nextBetween(0, OUTPUT_FIRST));
                if (!stack.isEmpty()) {
                    ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0,
                            (Math.random() - 0.5) * 0.2, 0.8, (Math.random() - 0.5) * 0.2, 2);
                    ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0,
                            (Math.random() - 0.5) * 0.2, 0, (Math.random() - 0.5) * 0.2, 2);
                }

                return;
            } else if (world.getTime() % 5 == 0) {
                ((ServerWorld) world).spawnParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 0,
                        (Math.random() - 0.5) * 0.2, 0.04, (Math.random() - 0.5) * 0.2, 0.3);
            }

            self.state = rot.getStateTextOrElse(TOO_SLOW_TEXT);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var stack = player.getMainHandStack();
        var copy = stack.copy();
        var x = FluidContainerUtil.interactWith(this.fluidContainer, (ServerPlayerEntity) player, player.getMainHandStack());
        if (x == null) {
            return super.onUse(state, world, pos, player, hit);
        }
        if (stack.isEmpty() && ItemStack.areEqual(stack, copy)) {
            return ActionResult.FAIL;
        }

        if (stack.isEmpty()) {
            player.setStackInHand(Hand.MAIN_HAND, x);
        } else if (!x.isEmpty()) {
            if (player.isCreative()) {
                if (!player.getInventory().contains(x)) {
                    player.getInventory().insertStack(x);
                }
            } else {
                player.getInventory().offerOrDrop(x);
            }
        }

        return ActionResult.SUCCESS_SERVER;
    }

    private void addToOutputOrDrop(ItemStack stack) {
        FactoryUtil.insertBetween(this, OUTPUT_FIRST, this.size(), stack);
        if (!stack.isEmpty()) {
            assert this.world != null;
            ItemScatterer.spawn(this.world, this.pos.getX() + 0.5, this.pos.getY()+ 0.5, this.pos.getZ() + 0.5, stack);
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
            var input = new MixingInput(this.stacks, FluidContainerInput.of(fluidContainer), world);
            return this.currentRecipe != null ?
                    MathHelper.clamp(this.currentRecipe.value().optimalSpeed(input) * 0.6 * this.speedScale,
                            this.currentRecipe.value().minimumSpeed(input) * 0.6,
                            this.currentRecipe.value().optimalSpeed(input) * 0.6
                    ) : 1;
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

    public CraftingRecipeInput asCraftingRecipeInput() {
        var l = new ArrayList<ItemStack>();
        for (int i = INPUT_FIRST; i < OUTPUT_FIRST; i++) {
            l.add(this.getStack(i));
        }

        return CraftingRecipeInput.create(2, 3, l);
    }

    public float temperature() {
        return this.temperature;
    }

    public FluidContainer getFluidContainer() {
        return this.fluidContainer;
    }

    @Override
    public FluidContainer getFluidContainer(Direction direction) {
        return this.fluidContainer;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.fluidContainer;
    }

    @Override
    public Text getFilledStateText() {
        return null;
    }

    private class Gui extends SimpleGui {
        private static final Text CURRENT_HEAT = Text.translatable("text.polyfactory.current_heat").styled(x -> x.withItalic(false));
        private int lastFluidUpdate = -1;
        private int delayTick = -1;

        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.updateTitleAndFluid();
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.MIXER));
            var fluidSlot = FluidContainerUtil.guiElement(fluidContainer, true);

            this.setSlot(1, fluidSlot);
            this.setSlot(1 + 9, fluidSlot);
            this.setSlot(1 + 9 * 2, fluidSlot);

            this.setSlotRedirect(2, new Slot(MixerBlockEntity.this, 0, 0, 0));
            this.setSlotRedirect(3, new Slot(MixerBlockEntity.this, 1, 1, 0));
            this.setSlotRedirect(2 + 9, new Slot(MixerBlockEntity.this, 2, 2, 0));
            this.setSlotRedirect(3 + 9, new Slot(MixerBlockEntity.this, 3, 3, 0));
            this.setSlotRedirect(2 + 18, new Slot(MixerBlockEntity.this, 4, 4, 0));
            this.setSlotRedirect(3 + 18, new Slot(MixerBlockEntity.this, 5, 5, 0));
            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            this.setSlot(4 + 9 + 9, GuiTextures.TEMPERATURE_OFFSET_RIGHT.getNamed(MathHelper.clamp(MixerBlockEntity.this.temperature, -1, 1), CURRENT_HEAT));
            this.setSlot(5 + 9 + 9, GuiElementBuilder.from(GuiTextures.EMPTY.getItemStack()).setName(CURRENT_HEAT));
            this.setSlotRedirect(6, new FurnaceOutputSlot(player, MixerBlockEntity.this, 6, 3, 0));
            this.setSlotRedirect(6 + 9, new FurnaceOutputSlot(player, MixerBlockEntity.this, 7, 3, 0));
            this.setSlotRedirect(6 + 18, new FurnaceOutputSlot(player, MixerBlockEntity.this, 8, 3, 0));
            this.open();
        }

        private void updateTitleAndFluid() {
            var text = GuiTextures.MIXER.apply(
                    Text.empty()
                            .append(Text.literal(GuiTextures.MIXER_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.MIXER.render(MixerBlockEntity.this.fluidContainer::provideRender))
                            .append(Text.literal(GuiTextures.MIXER_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(MixerBlockEntity.this.getCachedState().getBlock().getName())
            );


            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }


            this.lastFluidUpdate = MixerBlockEntity.this.fluidContainer.updateId();
        }

        private float progress() {
            return MixerBlockEntity.this.currentRecipe != null
                    ? (float) MathHelper.clamp(MixerBlockEntity.this.process / MixerBlockEntity.this.currentRecipe.value().time(
                            new MixingInput(stacks, FluidContainerInput.of(fluidContainer), world)), 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(MixerBlockEntity.this.pos)) > (18*18)) {
                this.close();
            }
            if (MixerBlockEntity.this.fluidContainer.updateId() != this.lastFluidUpdate && delayTick < 0) {
                delayTick = 3;
            }
            if (this.delayTick-- == 0) {
                this.updateTitleAndFluid();
            }

            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            this.setSlot(4 + 9 + 9, GuiTextures.TEMPERATURE_OFFSET_RIGHT.getNamed(MathHelper.clamp(MixerBlockEntity.this.temperature, -1, 1), CURRENT_HEAT));
            super.onTick();
        }
    }
}
