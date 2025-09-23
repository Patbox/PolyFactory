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
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sgui.virtual.inventory.VirtualSlot;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class PrimitiveSmelteryBlockEntity extends LockableBlockEntity implements MinimalSidedInventory, FluidOutput.ContainerBased {
    public static final int FLUID_CAPACITY = (int) (FluidConstants.INGOT * 9);
    private static final int[] ALL_SLOTS = IntStream.range(0, 1).toArray();
    private static final int[] INPUT_SLOTS = new int[]{0};
    private static final int[] FUEL_SLOTS = new int[]{1};
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private final FluidContainerImpl fluidContainer = new FluidContainerImpl(FLUID_CAPACITY, this::markDirty);
    public int fuelTicks = 0;
    public int fuelInitial = 1;
    private int progress = -1;
    private int progressEnd = 1;
    @SuppressWarnings("unchecked")
    private RecipeEntry<SmelteryRecipe> recipes;
    private ItemStack currentStack = ItemStack.EMPTY;

    public PrimitiveSmelteryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PRIMITIVE_SMELTERY, pos, state);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        var self = (PrimitiveSmelteryBlockEntity) t;

        FluidContainerUtil.tick(self.fluidContainer, (ServerWorld) world, pos, self.fuelTicks > 0 ? BlockHeat.LAVA : BlockHeat.NEUTRAL, self::addToOutputOrDrop);

        var dirty = false;

        var triesSmelting = false;

        {
            var stack = self.getStack(0);
            if (stack.isEmpty()) {
                self.progress = -1;
                self.currentStack = ItemStack.EMPTY;
                dirty = true;
            } else {
                var isDirtyStack = !ItemStack.areItemsAndComponentsEqual(self.currentStack, stack);
                if (!(!isDirtyStack && self.recipes == null)) {
                    var input = new SingleStackRecipeInput(stack);

                    var nullRecipe = self.recipes == null;

                    if (self.recipes == null || !self.recipes.value().matches(input, world)) {
                        self.recipes = world.getServer().getRecipeManager().getFirstMatch(FactoryRecipeTypes.SMELTERY, input, world).orElse(null);
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
                                stack.decrement(1);
                                self.progress = 0;

                                for (var x : result) {
                                    self.fluidContainer.insert(x, false);
                                }
                                if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayerEntity player) {
                                    TriggerCriterion.trigger(player, FactoryTriggers.SMELTERY_MELTS);
                                    Criteria.RECIPE_CRAFTED.trigger(player, self.recipes.id(), List.of());
                                }
                            }
                        } else if (self.world.getServer().getTicks() % 4 == 0) {
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

            if (!state.get(SteamEngineBlock.LIT)) {
                world.setBlockState(pos, state.with(SteamEngineBlock.LIT, true));
            }
        } else {
            var isFueled = false;
            if (triesSmelting) {
                var stack = self.getStack(1);

                if (!stack.isEmpty()) {
                    var value = world.getFuelRegistry().getFuelTicks(stack);
                    if (value > 0) {
                        var remainder = stack.getRecipeRemainder();
                        stack.decrement(1);
                        self.fuelTicks = value;
                        self.fuelInitial = self.fuelTicks;
                        isFueled = true;
                        if (stack.isEmpty()) {
                            self.setStack(1, ItemStack.EMPTY);
                        }

                        if (!remainder.isEmpty()) {
                            FactoryUtil.insertBetween(self, 9, 12, remainder);
                            if (!remainder.isEmpty()) {
                                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, remainder);
                            }
                        }

                        dirty = true;
                    }
                }

            }

            if (state.get(SteamEngineBlock.LIT) != isFueled) {
                world.setBlockState(pos, state.with(SteamEngineBlock.LIT, isFueled));
            }
        }

        if (dirty) {
            self.markDirty();
        }
    }

    private void addToOutputOrDrop(ItemStack stack) {
        FactoryUtil.insertBetween(this, 0, 9, stack);
        if (!stack.isEmpty()) {
            assert this.world != null;
            ItemScatterer.spawn(this.world, this.pos.getX() + 0.5, this.pos.getY() + 2, this.pos.getZ() + 0.5, stack);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        Inventories.writeData(view, this.items);
        this.fluidContainer.writeData(view, "fluids");
        view.putInt("fuel_ticks", this.fuelTicks);
        view.putInt("fuel_initial", this.fuelInitial);
        super.writeData(view);
    }

    @Override
    public void readData(ReadView view) {
        Inventories.readData(view, items);
        this.fluidContainer.readData(view, "fluids");
        this.fuelInitial = view.getInt("fuel_initial", 0);
        this.fuelTicks = view.getInt("fuel_ticks", 0);
        super.readData(view);
        this.currentStack = ItemStack.EMPTY;
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.items;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.world != null && (slot == 1 == this.world.getFuelRegistry().isFuel(stack));
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return !this.world.getFuelRegistry().isFuel(stack) && slot == 1;
    }

    public void createGui(ServerPlayerEntity player) {
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
    public void markDirty() {
        super.markDirty();
        if (this.world != null) {
            this.world.updateComparators(this.getPos().up(), this.getCachedState().getBlock());
        }
    }


    private class Gui extends SimpleGui {
        private final Slot inputSlot;
        private final FuelSlot fuelSlot;
        private boolean active;
        private int lastFluidUpdate = -1;
        private int delayTick = -1;

        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.inputSlot = new Slot(PrimitiveSmelteryBlockEntity.this, 0, 0, 0);
            this.fuelSlot = new FuelSlot(PrimitiveSmelteryBlockEntity.this, 1, player.getEntityWorld().getFuelRegistry());
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
                    Text.empty()
                            .append(Text.literal(GuiTextures.SMELTERY_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.PRIMITIVE_SMELTERY.render(PrimitiveSmelteryBlockEntity.this.fluidContainer::provideRender))
                            .append(Text.literal(GuiTextures.SMELTERY_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(PrimitiveSmelteryBlockEntity.this.getCachedState().getBlock().getName())
            );


            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }


            this.lastFluidUpdate = PrimitiveSmelteryBlockEntity.this.fluidContainer.updateId();
        }

        private void updateSmeltingProgress() {
            var progress = MathHelper.clamp((float) PrimitiveSmelteryBlockEntity.this.progress / PrimitiveSmelteryBlockEntity.this.progressEnd, 0, 1);
            this.setSlot(9 + 3, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress));
        }


        private float fuelProgress() {
            return PrimitiveSmelteryBlockEntity.this.fuelInitial > 0
                    ? MathHelper.clamp(PrimitiveSmelteryBlockEntity.this.fuelTicks / (float) PrimitiveSmelteryBlockEntity.this.fuelInitial, 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(PrimitiveSmelteryBlockEntity.this.pos)) > (18 * 18)) {
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
            if (slot != null && slot.hasStack() && !(slot instanceof VirtualSlot)) {
                ItemStack itemStack2 = slot.getStack();
                itemStack = itemStack2.copy();
                if (index < this.getVirtualSize()) {
                    if (!this.insertItem(itemStack2, this.getVirtualSize(), this.getVirtualSize() + 36, true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.player.getEntityWorld().getFuelRegistry().isFuel(itemStack2)) {
                    if (!FactoryUtil.insertItemIntoSlots(itemStack2, List.of(this.fuelSlot), false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!FactoryUtil.insertItemIntoSlots(itemStack2, List.of(this.inputSlot), false)) {
                        return ItemStack.EMPTY;
                    }
                }

                if (itemStack2.isEmpty()) {
                    slot.setStack(ItemStack.EMPTY);
                } else {
                    slot.markDirty();
                }
            } else if (slot instanceof VirtualSlot) {
                return slot.getStack();
            }

            return itemStack;
        }
    }
}
