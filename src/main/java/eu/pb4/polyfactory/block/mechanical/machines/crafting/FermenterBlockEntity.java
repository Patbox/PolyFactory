package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.FluidOutput;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.block.other.ItemOutputBufferBlock;
import eu.pb4.polyfactory.block.other.OutputContainerOwner;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.fermenting.FermentingRecipe;
import eu.pb4.polyfactory.recipe.input.SingleItemWithTemperature;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.CrafterLikeInsertContainer;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.polyfactory.util.inventory.SubContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class FermenterBlockEntity extends TallItemMachineBlockEntity implements FluidOutput.ContainerBased, OutputContainerOwner, MinimalSidedContainer, CrafterLikeInsertContainer, BlockEntityExtraListener {

    public static final int OUTPUT_FIRST = 6;
    public static final int INPUT_FIRST = 0;
    public static final long FLUID_CAPACITY = FluidType.BLOCK_AMOUNT * 2;
    public static final int[] OUTPUT_SLOTS = {6, 7, 8, 9, 10, 11};
    public static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5};
    protected float temperature = 0;

    private boolean active;
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(12, ItemStack.EMPTY);
    private final Container outputContainer = new SubContainer(this, OUTPUT_FIRST);

    private final RecipeHolder<FermentingRecipe>[] recipes = new RecipeHolder[INPUT_SLOTS.length];
    private final ItemStack[] currentStacks = new ItemStack[INPUT_SLOTS.length];
    protected double[] progress = new double[INPUT_SLOTS.length];
    protected double[] progressEnd = new double[INPUT_SLOTS.length];


    private final FluidContainerImpl fluidContainer = new FluidContainerImpl(FLUID_CAPACITY, this::setChanged);
    private FermenterBlock.Model model;
    private boolean inventoryChanged = false;
    private double speedScale;

    public FermenterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.FERMENTER, pos, state);
        Arrays.fill(this.progress, 0);
        Arrays.fill(this.progressEnd, 1);
        Arrays.fill(this.currentStacks, ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.stacks);
        //view.putDouble("progress", this.process);
        this.fluidContainer.writeData(view, "fluid");
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, this.stacks);
        //.process = view.getDoubleOr("Progress", 0);
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
        var facing = this.getBlockState().getValue(FermenterBlock.INPUT_FACING);
        return facing.getOpposite() == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot < OUTPUT_FIRST;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot >= OUTPUT_FIRST;
    }

    public void createGui(ServerPlayer player) {
        new Gui(player);
    }

    public static <T extends BlockEntity> void ticker(Level level, BlockPos pos, BlockState state, T t) {
        var self = (FermenterBlockEntity) t;
        var serverLevel = (ServerLevel) level;

        self.state = null;
        var oldTemp = self.temperature;
        self.temperature = BlockHeat.getReceived(level, pos);

        var forceRecipeRefresh = oldTemp != self.temperature;

        FluidContainerUtil.tick(self.fluidContainer, (ServerLevel) level, pos, self.temperature, self::addToOutputOrDrop);

        if (self.isInputEmpty() && self.fluidContainer.isEmpty()) {
            Arrays.fill(self.progress, 0d);
            self.speedScale = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        var dirty = false;
        var stuck = false;

        self.active = true;
        self.model.setActive(true);
        var rot = RotationUser.getRotation(serverLevel, pos.above());
        var fullSpeed = rot.speed();
        self.model.rotate((float) fullSpeed);
        self.model.tick();

        var speed = Math.min(Math.abs(fullSpeed) + 10, 200) / 200;
        self.speedScale = speed;

        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            var stack = self.stacks.get(i);
            if (stack.isEmpty()) {
                self.progress[i] = -1;
                self.currentStacks[i] = ItemStack.EMPTY;
                dirty = true;
                continue;
            }

            var isDirtyStack = !ItemStack.isSameItemSameComponents(self.currentStacks[i], stack);
            if (!isDirtyStack && self.recipes[i] == null && !forceRecipeRefresh) {
                continue;
            }

            var input = new SingleItemWithTemperature(stack, self.temperature, serverLevel);

            var nullRecipe = self.recipes[i] == null;

            if (self.recipes[i] == null || !self.recipes[i].value().matches(input, level)) {
                self.recipes[i] = level.getServer().getRecipeManager().getRecipeFor(FactoryRecipeTypes.FERMENTER, input, level).orElse(null);
                if (self.recipes[i] == null) {
                    self.progress[i] = -1;
                } else {
                    if (!nullRecipe) {
                        self.progress[i] = 0;
                    }
                    self.progressEnd[i] = self.recipes[i].value().time(input);
                }
                dirty = true;
                self.currentStacks[i] = stack.copyWithCount(1);
                continue;
            }

            if (isDirtyStack) {
                self.progress[i] = -1;
                self.currentStacks[i] = stack.copyWithCount(1);
                dirty = true;
                continue;
            }

            var preventFinish = false;

            var result = self.recipes[i].value().fluidOutput(input);
            var stored = self.fluidContainer.stored();

            for (var x : result) {
                stored += x.amount();
            }

            if (stored > self.fluidContainer.capacity()) {
                preventFinish = true;
            }

            var output = self.getOutputContainer();
            // Check space
            if (!stuck) {
                var inv = new SimpleContainer(output.getContainerSize());
                for (int a = 0; a < output.getContainerSize(); a++) {
                    inv.setItem(a, output.getItem(i).copy());
                }

                for (var item : self.recipes[i].value().assembleStacks(input, level.getRandom(), false)) {
                    FactoryUtil.tryInsertingRegular(inv, item);

                    if (!item.isEmpty()) {
                        preventFinish = true;
                        break;
                    }
                }
                var leftover = self.recipes[i].value().getRemainingItem(input, level.getRandom());
                FactoryUtil.tryInsertingRegular(inv, leftover);

                if (!leftover.isEmpty()) {
                    preventFinish = true;
                }
            }

            if (self.progress[i] < self.recipes[i].value().time(input)) {
                self.progress[i] += speed;
                dirty = true;
                continue;
            } else if (preventFinish) {
                stuck = true;
                continue;
            }

            dirty = true;
            self.progress[i] = 0;

            for (var x : result) {
                self.fluidContainer.insert(x, false);
            }

            for (var out : self.recipes[i].value().assembleStacks(input, level.getRandom(), true)) {
                FactoryUtil.tryInsertingRegular(output, out.copy());
            }

            FactoryUtil.tryInsertingRegular(output, self.recipes[i].value().getRemainingItem(input, level.getRandom()));

            stack.shrink(1);

            if (FactoryUtil.getClosestPlayer(level, pos, 32) instanceof ServerPlayer player) {
                TriggerCriterion.trigger(player, FactoryTriggers.FERMENTER_FERMENTS);
                CriteriaTriggers.RECIPE_CRAFTED.trigger(player, self.recipes[i].id(), List.of());
            }
        }

        if (dirty) {
            self.setChanged();
        }

        self.state = stuck ? OUTPUT_FULL_TEXT : null;
    }

    @Override
    public Container getOwnOutputContainer() {
        return this.outputContainer;
    }

    @Override
    public Container getOutputContainer() {
        return ItemOutputBufferBlock.getOutputContainer(this.outputContainer, this.level, this.getBlockPos(), this.getBlockState().getValue(FermenterBlock.INPUT_FACING).getOpposite());
    }

    @Override
    public boolean isOutputConnectedTo(Direction dir) {
        return this.getBlockState().getValue(FermenterBlock.INPUT_FACING).getOpposite() == dir;
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var stack = player.getMainHandItem();
        var copy = stack.copy();
        var x = FluidContainerUtil.interactWith(this.fluidContainer, (ServerPlayer) player, player.getMainHandItem(), false, true);
        if (x == null) {
            return super.onUse(state, world, pos, player, hit);
        }
        if (stack.isEmpty() && ItemStack.matches(stack, copy)) {
            return InteractionResult.FAIL;
        }

        if (stack.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, x);
        } else if (!x.isEmpty()) {
            if (player.isCreative()) {
                if (!player.getInventory().contains(x)) {
                    player.getInventory().add(x);
                }
            } else {
                player.getInventory().placeItemBackInInventory(x);
            }
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    private void addToOutputOrDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        FactoryUtil.tryInsertingRegular(this.getOutputContainer(), stack);
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
            return 5;
        }
        return 0;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.inventoryChanged = true;
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
    public Component getFilledStateText() {
        return null;
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int inputSize() {
        return 6;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        if (BlockAwareAttachment.get(chunk, this.getBlockPos()) instanceof HolderAttachment attachment && attachment.holder() instanceof FermenterBlock.Model model) {
            this.model = model;
        }
    }

    private class Gui extends SimpleGui {
        private static final Component CURRENT_HEAT = Component.translatable("text.polyfactory.current_heat").withStyle(x -> x.withItalic(false));
        private int lastFluidUpdate = -1;
        private int delayTick = -1;

        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.updateTitleAndFluid();
            this.setSlot(9 * 2, PolydexCompat.getButton(FactoryRecipeTypes.FERMENTER));
            var fluidSlot = FluidContainerUtil.guiElement(fluidContainer, false, true);
            this.updateFermentingProgress();

            this.setSlot(7, fluidSlot);
            this.setSlot(7 + 9, fluidSlot);
            this.setSlot(7 + 9 * 2, fluidSlot);

            this.setSlot(1, new Slot(FermenterBlockEntity.this, 0, 0, 0));
            this.setSlot(2, new Slot(FermenterBlockEntity.this, 1, 1, 0));
            this.setSlot(3, new Slot(FermenterBlockEntity.this, 2, 2, 0));
            this.setSlot(1 + 9, new Slot(FermenterBlockEntity.this, 3, 3, 0));
            this.setSlot(2 + 9, new Slot(FermenterBlockEntity.this, 4, 4, 0));
            this.setSlot(3 + 9, new Slot(FermenterBlockEntity.this, 5, 5, 0));

            this.setSlot(2 * 9 + 2, GuiTextures.TEMPERATURE.getNamed(Mth.clamp(FermenterBlockEntity.this.temperature, -1, 1), CURRENT_HEAT));

            this.setSlot(5, new FurnaceResultSlot(player, FermenterBlockEntity.this, 6, 3, 0));
            this.setSlot(6, new FurnaceResultSlot(player, FermenterBlockEntity.this, 7, 3, 0));
            this.setSlot(5 + 9, new FurnaceResultSlot(player, FermenterBlockEntity.this, 8, 3, 0));
            this.setSlot(6 + 9, new FurnaceResultSlot(player, FermenterBlockEntity.this, 9, 3, 0));
            this.setSlot(5 + 18, new FurnaceResultSlot(player, FermenterBlockEntity.this, 10, 3, 0));
            this.setSlot(6 + 18, new FurnaceResultSlot(player, FermenterBlockEntity.this, 11, 3, 0));
            this.open();
        }

        private void updateTitleAndFluid() {
            var text = GuiTextures.FERMENTER.apply(
                    Component.empty()
                            .append(Component.literal(GuiTextures.FERMENTER_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.MIXER.render(FermenterBlockEntity.this.fluidContainer::provideRender))
                            .append(Component.literal(GuiTextures.FERMENTER_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(FermenterBlockEntity.this.getBlockState().getBlock().getName())
            );


            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }


            this.lastFluidUpdate = FermenterBlockEntity.this.fluidContainer.updateId();
        }

        private void updateFermentingProgress() {
            for (int y = 0; y < 2; y++) {
                var progress = new float[4];
                var enabled = new boolean[4];
                var color = new int[4];
                for (int x = 0; x < 3; x++) {
                    int i = y * 3 + x;
                    progress[x] = (float) ((float) FermenterBlockEntity.this.progress[i] / FermenterBlockEntity.this.progressEnd[i]);
                    enabled[x] = FermenterBlockEntity.this.progress[i] > -1 && !FermenterBlockEntity.this.getItem(i).isEmpty();
                    color[x] = ARGB.srgbLerp(progress[x], 0xFFFFFF, 0x90ad10);
                }
                enabled[3] = true;
                this.setSlot(y * 9 + 4, new GuiElementBuilder(GuiTextures.LEFT_SHIFTED_3_BARS.get())
                        .hideTooltip()
                        .setCustomModelData(FloatList.of(progress), BooleanList.of(enabled), List.of(), IntList.of(color)));
            }
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(FermenterBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            if (FermenterBlockEntity.this.fluidContainer.updateId() != this.lastFluidUpdate && delayTick < 0) {
                delayTick = 3;
            }
            if (this.delayTick-- == 0) {
                this.updateTitleAndFluid();
            }

            this.updateFermentingProgress();
            this.setSlot(2 * 9 + 2, GuiTextures.TEMPERATURE.getNamed(Mth.clamp(FermenterBlockEntity.this.temperature, -1, 1), CURRENT_HEAT));
            super.onTick();
        }
    }
}
