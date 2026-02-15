package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlock;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.block.other.ItemOutputBufferBlock;
import eu.pb4.polyfactory.block.other.OutputContainerOwner;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.drain.DrainRecipe;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.TagLimitedSlot;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SubContainer;
import eu.pb4.polyfactory.util.movingitem.SimpleMovingItemContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class MDrainBlockEntity extends TallItemMachineBlockEntity implements FluidInputOutput.ContainerBased, OutputContainerOwner {

    public static final int CATALYST_FIRST = 2;
    public static final int OUTPUT_FIRST = 1;
    public static final int INPUT_FIRST = 0;
    public static final long FLUID_CAPACITY = FluidType.BLOCK_AMOUNT;
    private static final int[] OUTPUT_SLOTS = {1};
    private static final int[] INPUT_SLOTS = {0};
    private final SimpleMovingItemContainer[] containers = new SimpleMovingItemContainer[]{
            new SimpleMovingItemContainer(0, this::addMoving, this::removeMoving),
            new SimpleMovingItemContainer(1, this::addMoving, this::removeMoving),
            new SimpleMovingItemContainer()
    };
    private final Container outputContainer = new SubContainer(this, OUTPUT_FIRST);
    protected double process = 0;
    protected double speedScale = 0;
    @Nullable
    protected RecipeHolder<DrainRecipe> currentRecipe = null;
    private boolean active;
    private MDrainBlock.Model model;
    private boolean inventoryChanged = false;
    private final FluidContainerImpl fluidContainer = new FluidContainerImpl(FLUID_CAPACITY, this::setChanged);
    private float visualProgress;

    public MDrainBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.MECHANICAL_DRAIN, pos, state);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (MDrainBlockEntity) t;
        var serverWorld = (ServerLevel) world;

        if (self.model == null) {
            self.model = (MDrainBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            for (int i = 0; i < CATALYST_FIRST; i++) {
                self.updatePosition(i);
                self.containers[i].maybeAdd(self.model);
            }
        }
        self.state = null;
        var temp = BlockHeat.getReceived(world, pos) + self.fluidContainer.fluidTemperature();

        FluidContainerUtil.tick(self.fluidContainer, (ServerLevel) world, pos, temp, self::addToOutputOrDrop);
        self.model.setFluid(self.fluidContainer.topFluid(), self.fluidContainer.getFilledPercentage());

        if (self.isInputEmpty()) {
            self.process = 0;
            self.visualProgress = 0;
            self.updateInputPosition();
            self.speedScale = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        if (self.currentRecipe == null && !self.inventoryChanged) {
            self.process = 0;
            self.visualProgress = 0;
            self.updateInputPosition();
            self.speedScale = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            self.state = INCORRECT_ITEMS_TEXT;
            return;
        }
        var inputStack = self.getItem(INPUT_FIRST);

        var input = self.asInput();

        if (self.currentRecipe == null || !self.currentRecipe.value().matches(input, world)) {
            self.process = 0;
            self.visualProgress = 0;
            self.updateInputPosition();
            self.speedScale = 0;
            self.currentRecipe = serverWorld.recipeAccess().getRecipeFor(FactoryRecipeTypes.DRAIN, input, world).orElse(null);

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
        var rot = RotationUser.getRotation(world, pos.above());
        var fullSpeed = rot.speed();
        self.model.rotate((float) fullSpeed);
        self.model.tick();

        if (self.process >= self.currentRecipe.value().time(input)) {
            var itemOut = self.currentRecipe.value().assemble(input, world.registryAccess());

            var outputContainer = self.getOutputContainer();

            {
                var items = new ArrayList<ItemStack>();
                items.add(itemOut.copy());

                var inv = new net.minecraft.world.SimpleContainer(outputContainer.getContainerSize());
                for (int i = 0; i < outputContainer.getContainerSize(); i++) {
                    inv.setItem(i, outputContainer.getItem(i).copy());
                }

                for (var item : items) {
                    FactoryUtil.tryInsertingInv(inv, item, null);

                    if (!item.isEmpty()) {
                        self.state = OUTPUT_FULL_TEXT;
                        return;
                    }
                }
            }

            FactoryUtil.tryInsertingRegular(outputContainer, itemOut);

            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, self.currentRecipe.id(), List.of(inputStack.copy(), self.catalyst()));
            }

            inputStack.shrink(self.currentRecipe.value().decreasedInputItemAmount(input));
            if (inputStack.isEmpty()) {
                self.setItem(INPUT_FIRST, ItemStack.EMPTY);
            }
            for (var fluid : self.currentRecipe.value().fluidInput(input)) {
                self.fluidContainer.extract(fluid, false);
            }
            for (var fluid : self.currentRecipe.value().fluidOutput(input)) {
                self.fluidContainer.insert(fluid, false);
            }
            world.playSound(null, pos, self.currentRecipe.value().soundEvent().value(), SoundSource.BLOCKS);
            self.process = 0;
            self.visualProgress = 0;
            self.updateInputPosition();
            self.setChanged();
        } else {
            var strength = fullSpeed / 50 / 20;
            var speed = Math.min(Math.abs(strength) * 1, 1);
            self.speedScale = speed;
            if (speed > 0) {
                self.process += speed;
                self.visualProgress = (float) (self.process / self.currentRecipe.value().time(input));
                setChanged(world, pos, self.getBlockState());
                var ppos = self.containers[0].getPos();
                var fluid = Util.getRandomSafe(self.currentRecipe.value().fluidOutput(input), world.random);

                if (fluid.isPresent() && ppos != null) {
                    ((ServerLevel) world).sendParticles(fluid.get().instance().particle(),
                            ppos.x, ppos.y, ppos.z, 0,
                            0, -1, 0, 0.1);
                }
                self.updateInputPosition();
                return;
            }

            self.state = rot.getStateTextOrElse(TOO_SLOW_TEXT);
        }
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

    private DrainInput asInput() {
        return DrainInput.of(this.getItem(0).copy(), this.catalyst(), this.fluidContainer, false);
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
            Vec3 base = Vec3.atCenterOf(this.worldPosition);
            Quaternionf rot;
            var dir = this.getBlockState().getValue(MDrainBlock.INPUT_FACING);
            if (id == INPUT_FIRST) {
                base = base.add(0, 0.50, 0).relative(dir, -(this.visualProgress - 0.5) * (8 / 16f)).relative(dir.getClockWise(), -4 / 16f);
                rot = Direction.UP.getRotation()
                        .rotateY(-dir.getClockWise().toYRot() * Mth.DEG_TO_RAD)
                        .rotateX(Mth.PI * 3 / 4);
            } else {
                base = base.add(0, 0.45, 0).relative(dir, -0.3);
                rot = dir.getOpposite().getRotation();
            }

            container.setPos(base);
            container.scale(0.75f);
            container.setRotation(rot);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        this.writeInventoryView(view);
        view.putDouble("Progress", this.process);
        this.fluidContainer.writeData(view, "fluid");
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.readInventoryView(view);
        this.process = view.getDoubleOr("Progress", 0);
        this.fluidContainer.readData(view, "fluid");
        this.inventoryChanged = true;
        super.loadAdditional(view);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var f = components.get(FactoryDataComponents.FLUID);
        if (f != null) {
            this.fluidContainer.clear();
            f.extractTo(this.fluidContainer);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
        componentMapBuilder.set(FactoryDataComponents.FLUID, FluidComponent.copyFrom(this.fluidContainer));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard("fluid");
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        var facing = this.getBlockState().getValue(MDrainBlock.INPUT_FACING);
        return facing.getOpposite() == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot != OUTPUT_FIRST;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_FIRST;
    }

    public void createGui(ServerPlayer player) {
        new Gui(player);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == CATALYST_FIRST) {
            this.model.setCatalyst(stack);
        }
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if ((stack.isEmpty() || (ItemStack.isSameItemSameComponents(stack, this.catalyst()) && stack.getCount() < stack.getMaxStackSize()))
                && hit.getDirection() == Direction.UP && !this.catalyst().isEmpty()) {
            if (stack.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, this.catalyst());
            } else {
                stack.grow(1);
            }
            this.setCatalyst(ItemStack.EMPTY);
            return InteractionResult.SUCCESS_SERVER;
        } else if (stack.is(FactoryItemTags.DRAIN_CATALYST) && hit.getDirection() == Direction.UP && this.catalyst().isEmpty()) {
            this.setCatalyst(stack.copyWithCount(1));
            stack.consume(1, player);
            return InteractionResult.SUCCESS_SERVER;
        }

        if (world instanceof ServerLevel serverWorld) {
            var container = this.getFluidContainer();
            var copy = stack.copy();
            var input = DrainInput.of(copy, this.catalyst(), container, !(player instanceof FakePlayer));
            var optional = serverWorld.recipeAccess().getRecipeFor(FactoryRecipeTypes.DRAIN, input, world);
            if (optional.isEmpty()) {
                return super.onUse(state, world, pos, player, hit);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, optional.get().id(), List.of(stack.copy(), this.catalyst()));
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.DRAIN_USE);
            }

            var recipe = optional.get().value();
            var itemOut = recipe.assemble(input, player.registryAccess());
            for (var fluid : recipe.fluidInput(input)) {
                container.extract(fluid, false);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, FactoryUtil.exchangeStack(stack, recipe.decreasedInputItemAmount(input), player, itemOut));
            for (var fluid : recipe.fluidOutput(input)) {
                container.insert(fluid, false);
            }
            world.playSound(null, pos, recipe.soundEvent().value(), SoundSource.BLOCKS);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private void addToOutputOrDrop(ItemStack stack) {
        FactoryUtil.insertBetween(this, OUTPUT_FIRST, this.getContainerSize(), stack);
        if (!stack.isEmpty()) {
            assert this.level != null;
            Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, stack);
        }
    }

    private boolean isInputEmpty() {
        for (int i = 0; i < OUTPUT_FIRST; i++) {
            if (!this.getItem(i).isEmpty()) {
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
    public void setChanged() {
        super.setChanged();
        this.inventoryChanged = true;
    }

    @Override
    public SimpleMovingItemContainer[] getContainers() {
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

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.fluidContainer;
    }


    public ItemStack catalyst() {
        return this.getItem(CATALYST_FIRST);
    }

    public void setCatalyst(ItemStack catalyst) {
        this.setItem(CATALYST_FIRST, catalyst);
        if (this.model != null) {
            this.model.setCatalyst(catalyst);
        }
        this.setChanged();
    }

    public int getComparatorOutput(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (state.getValue(MDrainBlock.PART) == TallItemMachineBlock.Part.TOP) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
        }
        return (int) ((this.fluidContainer.stored() * 15) / this.fluidContainer.capacity());
    }

    private class Gui extends SimpleGui {
        private int lastFluidUpdate = -1;
        private int delayTick = -1;

        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.updateTitleAndFluid();
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.DRAIN));
            var fluidSlot = FluidContainerUtil.guiElement(fluidContainer, true);


            //noinspection PointlessArithmeticExpression
            this.setSlot(6 + 0 * 9, fluidSlot);
            //noinspection PointlessArithmeticExpression
            this.setSlot(6 + 1 * 9, fluidSlot);
            this.setSlot(6 + 2 * 9, fluidSlot);

            this.setSlotRedirect(2, new Slot(MDrainBlockEntity.this, 0, 0, 0));
            this.setSlotRedirect(9 + 5, new FurnaceResultSlot(player, MDrainBlockEntity.this, 1, 1, 0));
            this.setSlotRedirect(9 * 2 + 2, new TagLimitedSlot(MDrainBlockEntity.this, 2, FactoryItemTags.DRAIN_CATALYST) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return 1;
                }
            });
            this.setSlot(9 + 3, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            this.open();
        }

        private void updateTitleAndFluid() {
            var text = GuiTextures.MECHANICAL_DRAIN.apply(
                    Component.empty()
                            .append(Component.literal(GuiTextures.MECHANICAL_DRAIN_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.MIXER.render(MDrainBlockEntity.this.fluidContainer::provideRender))
                            .append(Component.literal(GuiTextures.MECHANICAL_DRAIN_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(MDrainBlockEntity.this.getBlockState().getBlock().getName())
            );


            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }


            this.lastFluidUpdate = MDrainBlockEntity.this.fluidContainer.updateId();
        }

        private float progress() {
            return MDrainBlockEntity.this.visualProgress;
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(MDrainBlockEntity.this.worldPosition)) > (18 * 18)) {
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
