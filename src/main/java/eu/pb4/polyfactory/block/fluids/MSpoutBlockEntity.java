package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import eu.pb4.polyfactory.recipe.spout.SpoutRecipe;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
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
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

public class MSpoutBlockEntity extends TallItemMachineBlockEntity {

    public static final int OUTPUT_FIRST = 1;
    public static final int INPUT_FIRST = 0;
    private static final int[] OUTPUT_SLOTS = {1};
    private static final int[] INPUT_SLOTS = {0};
    private final SimpleContainer[] containers = new SimpleContainer[]{
            new SimpleContainer(0, this::addMoving, this::removeMoving),
            new SimpleContainer(1, this::addMoving, this::removeMoving)
    };
    protected double process = 0;
    protected double speedScale = 0;
    @Nullable
    protected RecipeEntry<SpoutRecipe> currentRecipe = null;
    private boolean active;
    private MSpoutBlock.Model model;
    private boolean inventoryChanged = false;
    private int containerUpdateId = -1;
    @Nullable
    private FluidContainer fluidContainer;
    private boolean isCooling = false;

    public MSpoutBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.MECHANICAL_SPOUT, pos, state);
    }

    private SingleItemWithFluid asInput() {
        return SingleItemWithFluid.of(this.getStack(INPUT_FIRST).copy(), fluidContainer, (ServerWorld) world);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (MSpoutBlockEntity) t;

        if (self.model == null) {
            self.model = (MSpoutBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            for (int i = 0; i < self.containers.length; i++) {
                self.updatePosition(i);
                self.containers[i].maybeAdd(self.model);
            }
        }

        var alt = !self.containers[0].isContainerEmpty() && self.containers[0].getContainer().get().isIn(FactoryItemTags.SPOUT_ITEM_HORIZONTAL);
        self.model.altModel(alt);
        self.state = null;
        var posAbove = pos.up();

        var rot = RotationUser.getRotation(world, posAbove);
        var fullSpeed = rot.speed();
        var strength = fullSpeed / 60 / 20;
        NetworkComponent.Pipe.forEachLogic((ServerWorld) world, posAbove, l -> l.setSourceStrength(posAbove, strength));

        var container = world.getBlockEntity(pos.up(2)) instanceof FluidContainerOwner owner ? owner.getFluidContainer(Direction.DOWN) : null;

        if (self.isInputEmpty()) {
            self.process = 0;
            self.speedScale = 0;
            self.active = false;
            self.isCooling = false;
            self.model.setActive(false);
            self.model.tick();
            self.fluidContainer = container;
            return;
        }

        if (container == null || (self.currentRecipe == null && !self.inventoryChanged && self.fluidContainer == container && (self.containerUpdateId == container.updateId()))) {
            self.process = 0;
            self.speedScale = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            self.state = INCORRECT_ITEMS_TEXT;
            self.isCooling = false;
            self.fluidContainer = container;
            return;
        }

        self.containerUpdateId = container.updateId();
        self.fluidContainer = container;

        var inputStack = self.getStack(INPUT_FIRST);

        var input = self.asInput();

        if (self.currentRecipe == null || !self.currentRecipe.value().matches(input, world)) {
            self.process = 0;
            self.speedScale = 0;
            self.currentRecipe = ((ServerWorld) world).getRecipeManager().getFirstMatch(FactoryRecipeTypes.SPOUT, input, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.model.setActive(false);
                self.model.tick();
                self.inventoryChanged = false;
                self.isCooling = false;
                self.state = INCORRECT_ITEMS_TEXT;
                return;
            }
        }
        self.inventoryChanged = false;


        self.active = true;
        self.model.setActive(true);
        self.model.tick();

        var coolingTime = self.currentRecipe.value().coolingTime(input);

        var time = self.isCooling ? coolingTime : self.currentRecipe.value().time(input);

        if (self.process >= time) {
            if (coolingTime > 0 && !self.isCooling) {
                self.isCooling = true;
                self.process = 0;
                self.markDirty();
                return;
            }

            var itemOut = self.currentRecipe.value().craft(input, world.getRegistryManager());
            var currentOutput = self.getStack(OUTPUT_FIRST);
            if (currentOutput.isEmpty()) {
                self.setStack(OUTPUT_FIRST, itemOut);
            } else if (ItemStack.areItemsAndComponentsEqual(itemOut, currentOutput) && currentOutput.getCount() + itemOut.getCount() <= itemOut.getMaxCount()) {
                currentOutput.increment(itemOut.getCount());
            } else {
                return;
            }
            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayerEntity serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.SPOUT_CRAFT);
                Criteria.RECIPE_CRAFTED.trigger(serverPlayer, self.currentRecipe.id(), List.of(inputStack.copy()));
            }
            inputStack.decrement(self.currentRecipe.value().decreasedInputItemAmount(input));
            var damage = self.currentRecipe.value().damageInputItemAmount(input);

            if (damage > 0) {
                var x = inputStack.copy();
                inputStack.damage(damage, (ServerWorld) world, null, Consumers.nop());
                if (inputStack.isEmpty()) {
                    if (x.contains(DataComponentTypes.BREAK_SOUND)) {
                        world.playSound(null, pos, x.get(DataComponentTypes.BREAK_SOUND).value(), SoundCategory.BLOCKS);
                    }
                    ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, x),
                            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.51, 5,
                            0.2, 0, 0.2, 0.5);
                }
            }
            if (inputStack.isEmpty()) {
                self.setStack(INPUT_FIRST, ItemStack.EMPTY);
            }

            for (var fluid : self.currentRecipe.value().fluidInput(input)) {
                container.extract(fluid, false);
            }
            world.playSound(null, pos, self.currentRecipe.value().soundEvent().value(), SoundCategory.BLOCKS);
            self.process = 0;
            self.isCooling = false;
            self.model.setProgress(false, 0, null);
            self.markDirty();
        } else {
            var speed = Math.min(Math.abs(strength) * 1, 1);
            self.speedScale = speed;
            if (speed > 0 || self.isCooling) {
                self.process += self.isCooling ? 1 : speed;
                markDirty(world, pos, self.getCachedState());
                var fluid = Util.getRandomOrEmpty(self.currentRecipe.value().fluidInput(input), world.random);
                if (fluid.isPresent()) {
                    var progress = self.process / time;
                    if (!self.isCooling) {
                        ((ServerWorld) world).spawnParticles(fluid.get().instance().particle(),
                                pos.getX() + 0.5, pos.getY() + 0.5 + 1 + 4 / 16f, pos.getZ() + 0.5, 0,
                                0, -1, 0, 0.1);
                    }

                    self.model.setProgress(self.isCooling, progress, fluid.get().instance());

                }

                return;
            }


            self.state = rot.getStateTextOrElse(TOO_SLOW_TEXT);
        }
    }

    protected void updatePosition(int id) {
        var c = containers[id];

        if (!c.isContainerEmpty()) {
            assert c.getContainer() != null;
            var container = c.getContainer();
            Vec3d base = Vec3d.ofCenter(this.pos);
            var scale = 0.75f;
            Quaternionf rot;
            var dir = this.getCachedState().get(MDrainBlock.INPUT_FACING);
            if (id == INPUT_FIRST) {
                if (container.get().isIn(FactoryItemTags.SPOUT_ITEM_HORIZONTAL)) {
                    base = base.add(0, 7.5f / 16 - (2 / 16f / 16f), 0);
                    rot = RotationAxis.POSITIVE_Y.rotation(0).mul(dir.getOpposite().getRotationQuaternion());
                    scale = 2 * 12 / 16f;//1.25f;
                } else {
                    base = base.add(0, 10f / 16, 0);
                    rot = Direction.UP.getRotationQuaternion().rotateY(dir.getPositiveHorizontalDegrees() * MathHelper.RADIANS_PER_DEGREE);
                }
            } else {
                if (!containers[0].isContainerEmpty() && containers[0].getContainer().get().isIn(FactoryItemTags.SPOUT_ITEM_HORIZONTAL)) {
                    base = base.add(0, 1 / 16f, 0);
                }

                base = base.add(0, 7.5 / 16, 0).offset(dir, -0.4);
                rot = RotationAxis.POSITIVE_Y.rotation(MathHelper.PI).mul(dir.getOpposite().getRotationQuaternion());
            }

            container.setPos(base);
            container.scale(scale);
            container.setRotation(rot);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        this.writeInventoryView(view);
        view.putDouble("Progress", this.process);
        view.putBoolean("is_cooling", this.isCooling);
        super.writeData(view);
    }

    @Override
    public void readData(ReadView view) {
        this.readInventoryView(view);
        this.process = view.getDouble("Progress", 0);
        this.isCooling = view.getBoolean("is_cooling", false);
        this.inventoryChanged = true;
        super.readData(view);
    }

    @Override
    protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
    }

    @Override
    protected void addComponents(ComponentMap.Builder componentMapBuilder) {
        super.addComponents(componentMapBuilder);
    }

    @Override
    public void removeFromCopiedStackData(WriteView view) {
        super.removeFromCopiedStackData(view);
        view.remove("fluid");
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(MDrainBlock.INPUT_FACING);
        return facing.getOpposite() == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot != OUTPUT_FIRST;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_FIRST;
    }

    public void createGui(ServerPlayerEntity player) {
        new Gui(player);
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

    private class Gui extends SimpleGui {
        private int lastFluidUpdate = -1;
        private int delayTick = -1;
        private FluidContainer lastContainer;

        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.updateTitleAndFluid();
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.SPOUT));

            this.setSlotRedirect(9 + 3, new Slot(MSpoutBlockEntity.this, 0, 0, 0));
            this.setSlotRedirect(9 + 6, new FurnaceOutputSlot(player, MSpoutBlockEntity.this, 1, 1, 0));
            this.setSlot(9 + 4, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            this.open();
        }

        private void updateTitleAndFluid() {
            var text = fluidContainer != null ? GuiTextures.MECHANICAL_SPOUT.apply(
                    Text.empty()
                            .append(Text.literal(GuiTextures.MECHANICAL_SPOUT_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.MIXER.render(MSpoutBlockEntity.this.fluidContainer::provideRender))
                            .append(Text.literal(GuiTextures.MECHANICAL_SPOUT_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(MSpoutBlockEntity.this.getCachedState().getBlock().getName()))
                    : GuiTextures.MECHANICAL_SPOUT_NO_CONN.apply(MSpoutBlockEntity.this.getCachedState().getBlock().getName());

            var fluidSlot = FluidContainerUtil.guiElement(fluidContainer, false);

            //noinspection PointlessArithmeticExpression
            this.setSlot(2 + 0 * 9, fluidSlot);
            //noinspection PointlessArithmeticExpression
            this.setSlot(2 + 1 * 9, fluidSlot);
            this.setSlot(2 + 2 * 9, fluidSlot);

            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }

            this.lastFluidUpdate = fluidContainer != null ? fluidContainer.updateId() : -1;
            lastContainer = fluidContainer;
        }

        private float progress() {
            if (MSpoutBlockEntity.this.currentRecipe == null) {
                return 0;
            }
            var value = 0d;
            var coolingTime = MSpoutBlockEntity.this.currentRecipe.value().coolingTime(asInput());
            if (coolingTime > 0 && MSpoutBlockEntity.this.isCooling) {
                value = MSpoutBlockEntity.this.process / coolingTime * 0.5 + 0.5;
            } else if (coolingTime > 0) {
                value = MSpoutBlockEntity.this.process / MSpoutBlockEntity.this.currentRecipe.value().time(asInput()) * 0.5;
            } else {
                value = MSpoutBlockEntity.this.process / MSpoutBlockEntity.this.currentRecipe.value().time(asInput());
            }


            return (float) MathHelper.clamp(value, 0, 1);
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(MSpoutBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            if (fluidContainer != lastContainer) {
                delayTick = 0;
            } else if (((fluidContainer != null && fluidContainer.updateId() != this.lastFluidUpdate)) && delayTick < 0) {
                delayTick = 3;
            }
            if (this.delayTick-- == 0) {
                this.updateTitleAndFluid();
            }

            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            super.onTick();
        }
    }
}
