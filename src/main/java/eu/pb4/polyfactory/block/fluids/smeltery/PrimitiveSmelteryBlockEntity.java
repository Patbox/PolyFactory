package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.FluidOutput;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlock;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.smeltery.SmelteryRecipe;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.FuelSlot;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.virtual.inventory.VirtualSlot;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

public class PrimitiveSmelteryBlockEntity extends LockableBlockEntity implements MinimalSidedContainer, FluidOutput.ContainerBased {
    public static final int FLUID_CAPACITY = (int) (FluidConstants.INGOT * 9);
    private static final int[] ALL_SLOTS = IntStream.range(0, 1).toArray();
    private static final int[] INPUT_SLOTS = new int[]{0};
    private static final int[] FUEL_SLOTS = new int[]{1};
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final FluidContainerImpl fluidContainer = new FluidContainerImpl(FLUID_CAPACITY, this::setChanged);
    public int fuelTicks = 0;
    public int fuelInitial = 1;
    private int progress = -1;
    private int progressEnd = 1;
    @SuppressWarnings("unchecked")
    private RecipeHolder<SmelteryRecipe> recipes;
    private ItemStack currentStack = ItemStack.EMPTY;

    public PrimitiveSmelteryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PRIMITIVE_SMELTERY, pos, state);
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        var self = (PrimitiveSmelteryBlockEntity) t;

        FluidContainerUtil.tick(self.fluidContainer, (ServerLevel) world, pos, self.fuelTicks > 0 ? BlockHeat.LAVA : BlockHeat.NEUTRAL, self::addToOutputOrDrop);

        var dirty = false;

        var triesSmelting = false;

        {
            var stack = self.getItem(0);
            if (stack.isEmpty()) {
                self.progress = -1;
                self.currentStack = ItemStack.EMPTY;
                dirty = true;
            } else {
                var isDirtyStack = !ItemStack.isSameItemSameComponents(self.currentStack, stack);
                if (!(!isDirtyStack && self.recipes == null)) {
                    var input = new SingleRecipeInput(stack);

                    var nullRecipe = self.recipes == null;

                    if (self.recipes == null || !self.recipes.value().matches(input, world)) {
                        self.recipes = world.getServer().getRecipeManager().getRecipeFor(FactoryRecipeTypes.SMELTERY, input, world).orElse(null);
                        if (self.recipes == null) {
                            self.progress = -1;
                        } else {
                            if (!nullRecipe) {
                                self.progress = 0;
                            }
                            self.progressEnd = self.recipes.value().time(input, world) * 5 / 2;
                        }
                        dirty = true;
                        self.currentStack = stack.copyWithCount(1);
                    }

                    if (isDirtyStack) {
                        self.progress = -1;
                        self.currentStack = stack.copyWithCount(1);
                        dirty = true;
                    } else {
                        var result = self.recipes.value().output(input, world);
                        var stored = self.fluidContainer.stored();

                        for (var x : result) {
                            stored += x.amount();
                        }

                        if (stored <= self.fluidContainer.capacity()) {
                            triesSmelting = true;
                        }
                        if (self.fuelTicks > 0) {
                            if (self.progress < self.recipes.value().time(input, world) * 5 / 2) {
                                self.progress++;
                                dirty = true;
                            } else if (stored <= self.fluidContainer.capacity()) {
                                dirty = true;
                                stack.shrink(1);
                                self.progress = 0;

                                for (var x : result) {
                                    self.fluidContainer.insert(x, false);
                                }
                                if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayer player) {
                                    TriggerCriterion.trigger(player, FactoryTriggers.SMELTERY_MELTS);
                                    CriteriaTriggers.RECIPE_CRAFTED.trigger(player, self.recipes.id(), List.of());
                                }
                            }
                        } else if (self.level.getServer().getTickCount() % 4 == 0) {
                            var x = Math.max(self.progress - 5, -1);
                            if (x != self.progress) {
                                self.progress = x;
                                dirty = true;
                            }
                        }
                    }
                }
            }
        }

        if (self.fuelTicks > 0) {
            self.fuelTicks -= triesSmelting ? 4 : 2;

            if (!state.getValue(SteamEngineBlock.LIT)) {
                world.setBlockAndUpdate(pos, state.setValue(SteamEngineBlock.LIT, true));
            }
        } else {
            var isFueled = false;
            if (triesSmelting) {
                var stack = self.getItem(1);

                if (!stack.isEmpty()) {
                    var value = world.fuelValues().burnDuration(stack);
                    if (value > 0) {
                        var remainder = stack.getRecipeRemainder();
                        stack.shrink(1);
                        self.fuelTicks = value;
                        self.fuelInitial = self.fuelTicks;
                        isFueled = true;
                        if (stack.isEmpty()) {
                            self.setItem(1, ItemStack.EMPTY);
                        }

                        if (!remainder.isEmpty()) {
                            FactoryUtil.insertBetween(self, 9, 12, remainder);
                            if (!remainder.isEmpty()) {
                                Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, remainder);
                            }
                        }

                        dirty = true;
                    }
                }

            }

            if (state.getValue(SteamEngineBlock.LIT) != isFueled) {
                world.setBlockAndUpdate(pos, state.setValue(SteamEngineBlock.LIT, isFueled));
            }
        }

        if (dirty) {
            self.setChanged();
        }
    }

    private void addToOutputOrDrop(ItemStack stack) {
        FactoryUtil.insertBetween(this, 0, 9, stack);
        if (!stack.isEmpty()) {
            assert this.level != null;
            Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 2, this.worldPosition.getZ() + 0.5, stack);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.items);
        this.fluidContainer.writeData(view, "fluids");
        view.putInt("fuel_ticks", this.fuelTicks);
        view.putInt("fuel_initial", this.fuelInitial);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, items);
        this.fluidContainer.readData(view, "fluids");
        this.fuelInitial = view.getIntOr("fuel_initial", 0);
        this.fuelTicks = view.getIntOr("fuel_ticks", 0);
        super.loadAdditional(view);
        this.currentStack = ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.items;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.level != null && (slot == 1 == this.level.fuelValues().isFuel(stack));
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return !this.level.fuelValues().isFuel(stack) && slot == 1;
    }

    public void createGui(ServerPlayer player) {
        new Gui(player);
    }

    @Override
    public @Nullable FluidContainer getFluidContainer(Direction direction) {
        return this.fluidContainer;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.fluidContainer;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null) {
            this.level.updateNeighbourForOutputSignal(this.getBlockPos().above(), this.getBlockState().getBlock());
        }
    }


    private class Gui extends SimpleGui {
        private final Slot inputSlot;
        private final FuelSlot fuelSlot;
        private boolean active;
        private int lastFluidUpdate = -1;
        private int delayTick = -1;

        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.inputSlot = new Slot(PrimitiveSmelteryBlockEntity.this, 0, 0, 0);
            this.fuelSlot = new FuelSlot(PrimitiveSmelteryBlockEntity.this, 1, player.level().fuelValues());
            this.setSlotRedirect(2, this.inputSlot);
            this.setSlotRedirect(9 * 2 + 2, this.fuelSlot);

            for (int x = 0; x < 3; x++) {
                var fluid = FluidContainerUtil.guiElement(fluidContainer, false);
                for (int y = 0; y < 3; y++) {
                    this.setSlot(9 * y + 5 + x, fluid);
                }
            }
            this.setSlot(9 + 2, GuiTextures.FLAME.getCeil(fuelProgress()));
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.SMELTERY));
            this.active = PrimitiveSmelteryBlockEntity.this.fuelTicks > 0;
            this.updateTitleAndFluid();
            this.updateSmeltingProgress();

            this.open();
        }

        private void updateTitleAndFluid() {
            var text = GuiTextures.PRIMITIVE_SMELTERY.apply(
                    Component.empty()
                            .append(Component.literal(GuiTextures.SMELTERY_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.PRIMITIVE_SMELTERY.render(PrimitiveSmelteryBlockEntity.this.fluidContainer::provideRender))
                            .append(Component.literal(GuiTextures.SMELTERY_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(PrimitiveSmelteryBlockEntity.this.getBlockState().getBlock().getName())
            );


            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }


            this.lastFluidUpdate = PrimitiveSmelteryBlockEntity.this.fluidContainer.updateId();
        }

        private void updateSmeltingProgress() {
            var progress = Mth.clamp((float) PrimitiveSmelteryBlockEntity.this.progress / PrimitiveSmelteryBlockEntity.this.progressEnd, 0, 1);
            this.setSlot(9 + 3, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress));
        }


        private float fuelProgress() {
            return PrimitiveSmelteryBlockEntity.this.fuelInitial > 0
                    ? Mth.clamp(PrimitiveSmelteryBlockEntity.this.fuelTicks / (float) PrimitiveSmelteryBlockEntity.this.fuelInitial, 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(PrimitiveSmelteryBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }

            if (PrimitiveSmelteryBlockEntity.this.fluidContainer.updateId() != this.lastFluidUpdate && delayTick < 0) {
                delayTick = 3;
            }
            if (this.delayTick-- == 0) {
                this.updateTitleAndFluid();
            }
            this.updateSmeltingProgress();

            var active = PrimitiveSmelteryBlockEntity.this.fuelTicks > 0;
            if (!this.active && active) {
                this.active = true;
                TriggerCriterion.trigger(this.player, FactoryTriggers.FUEL_STEAM_ENGINE);
            }
            this.setSlot(9 + 2, GuiTextures.FLAME.getCeil(fuelProgress()));
            super.onTick();
        }

        @Override
        public ItemStack quickMove(int index) {
            ItemStack itemStack = ItemStack.EMPTY;
            Slot slot = this.getSlotRedirectOrPlayer(index);
            if (slot != null && slot.hasItem() && !(slot instanceof VirtualSlot)) {
                ItemStack itemStack2 = slot.getItem();
                itemStack = itemStack2.copy();
                if (index < this.getVirtualSize()) {
                    if (!this.insertItem(itemStack2, this.getVirtualSize(), this.getVirtualSize() + 36, true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.player.level().fuelValues().isFuel(itemStack2)) {
                    if (!FactoryUtil.insertItemIntoSlots(itemStack2, List.of(this.fuelSlot), false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!FactoryUtil.insertItemIntoSlots(itemStack2, List.of(this.inputSlot), false)) {
                        return ItemStack.EMPTY;
                    }
                }

                if (itemStack2.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            } else if (slot instanceof VirtualSlot) {
                return slot.getItem();
            }

            return itemStack;
        }
    }
}
