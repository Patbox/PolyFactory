package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.BlockHeat;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlock;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
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
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
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
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class SmelteryBlockEntity extends LockableBlockEntity implements MinimalSidedInventory, FluidOutput.ContainerBased {
    private static final int[] ALL_SLOTS = IntStream.range(0, 12).toArray();
    private static final int[] INPUT_SLOTS = IntStream.range(0, 9).toArray();
    private static final int[] FUEL_SLOTS = IntStream.range(9, 12).toArray();
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(12, ItemStack.EMPTY);
    private final int[] progress = new int[9];
    private final int[] progressEnd = new int[9];
    @SuppressWarnings("unchecked")
    private final RecipeEntry<SmelteryRecipe>[] recipes = new RecipeEntry[9];
    private final ItemStack[] currentStacks = new ItemStack[9];

    private final FluidContainerImpl fluidContainer = new FluidContainerImpl(FluidConstants.BUCKET * 12 * 2, this::markDirty);
    public int fuelTicks = 0;
    public int fuelInitial = 1;

    public SmelteryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.SMELTERY, pos, state);
        Arrays.fill(this.currentStacks, ItemStack.EMPTY);
        Arrays.fill(this.progress, -1);
        Arrays.fill(this.progressEnd, 1);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        var self = (SmelteryBlockEntity) t;

        FluidContainerUtil.tick(self.fluidContainer, (ServerWorld) world, pos, self.fuelTicks > 0 ? BlockHeat.LAVA : BlockHeat.NEUTRAL, self::addToOutputOrDrop);

        var dirty = false;

        if (self.fuelTicks > 0) {
            self.fuelTicks -= 1;

            if (!state.get(SteamEngineBlock.LIT)) {
                world.setBlockState(pos, state.with(SteamEngineBlock.LIT, true));
            }
        } else {
            for (int i = 9; i < 12; i++) {
                var stack = self.getStack(i);

                if (!stack.isEmpty()) {
                    var value = world.getFuelRegistry().getFuelTicks(stack);
                    if (value > 0) {
                        var remainder = stack.getRecipeRemainder();
                        stack.decrement(1);
                        self.fuelTicks = value / 2;
                        self.fuelInitial = self.fuelTicks;
                        if (stack.isEmpty()) {
                            self.setStack(i, ItemStack.EMPTY);
                        }

                        if (!remainder.isEmpty()) {
                            FactoryUtil.insertBetween(self, 9, 12, remainder);
                            if (!remainder.isEmpty()) {
                                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, remainder);
                            }
                        }

                        dirty = true;
                        break;
                    }
                }
            }

            if (state.get(SteamEngineBlock.LIT)) {
                world.setBlockState(pos, state.with(SteamEngineBlock.LIT, false));
            }
        }

        for (int i = 0; i < 9; i++) {
            var stack = self.items.get(i);
            if (stack.isEmpty()) {
                self.progress[i] = -1;
                self.currentStacks[i] = ItemStack.EMPTY;
                dirty = true;
                continue;
            }
            var isDirtyStack = !ItemStack.areItemsAndComponentsEqual(self.currentStacks[i], stack);
            if (!isDirtyStack && self.recipes[i] == null) {
                continue;
            }

            var input = new SingleStackRecipeInput(stack);

            var nullRecipe = self.recipes[i] == null;

            if (self.recipes[i] == null || !self.recipes[i].value().matches(input, world)) {
                self.recipes[i] = world.getServer().getRecipeManager().getFirstMatch(FactoryRecipeTypes.SMELTERY, input, world).orElse(null);
                if (self.recipes[i] == null) {
                    self.progress[i] = -1;
                } else {
                    if (!nullRecipe) {
                        self.progress[i] = 0;
                    }
                    self.progressEnd[i] = self.recipes[i].value().time(input, world);
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


            if (self.fuelTicks > 0) {
                if (self.progress[i] < self.recipes[i].value().time(input, world)) {
                    self.progress[i]++;
                    dirty = true;
                    continue;
                }

                var result = self.recipes[i].value().output(input, world);

                var stored = self.fluidContainer.stored();

                for (var x : result) {
                    stored += x.amount();
                }

                if (stored >= self.fluidContainer.capacity()) {
                    continue;
                }
                dirty = true;
                stack.decrement(1);
                self.progress[i] = 0;

                for (var x : result) {
                    self.fluidContainer.insert(x, false);
                }
            } else if (self.world.getServer().getTicks() % 4 == 0) {
                var x = Math.max(self.progress[i] - 1, -1);
                if (x != self.progress[i]) {
                    self.progress[i] = x;
                    dirty = true;
                }
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
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Inventories.writeNbt(nbt, this.items, lookup);
        nbt.put("fluids", this.fluidContainer.toNbt(lookup));
        nbt.putInt("FuelTicks", this.fuelTicks);
        nbt.putInt("FuelInitial", this.fuelInitial);
        super.writeNbt(nbt, lookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Inventories.readNbt(nbt, items, lookup);
        this.fluidContainer.fromNbt(lookup, nbt, "fluids");
        this.fuelInitial = nbt.getInt("FuelInitial", 0);
        this.fuelTicks = nbt.getInt("FuelTicks", 0);
        super.readNbt(nbt, lookup);
        Arrays.fill(this.currentStacks, ItemStack.EMPTY);
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
        return this.world != null && (slot >= 9 == this.world.getFuelRegistry().isFuel(stack));
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
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
    }

    private class Gui extends SimpleGui {
        private boolean active;
        private int lastFluidUpdate = -1;
        private int delayTick = -1;

        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X5, player, false);
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    this.setSlotRedirect(9 * y + 1 + x, new Slot(SmelteryBlockEntity.this, y * 3 + x, x, y));
                }
                var fluid = FluidContainerUtil.guiElement(fluidContainer, false);
                for (int y = 0; y < 5; y++) {
                    this.setSlot(9 * y + 5 + x, fluid);
                }
                this.setSlotRedirect(9 * 4 + 1 + x, new FuelSlot(SmelteryBlockEntity.this, 9 + x, player.getServerWorld().getFuelRegistry()));
            }
            this.setSlot(9 * 3 + 2, GuiTextures.FLAME.get(fuelProgress()));
            this.active = SmelteryBlockEntity.this.fuelTicks > 0;
            this.updateTitleAndFluid();
            this.updateSmeltingProgress();

            this.open();
        }

        private void updateTitleAndFluid() {
            var text = GuiTextures.SMELTERY.apply(
                    Text.empty()
                            .append(Text.literal(GuiTextures.SMELTERY_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.SMELTERY.render(SmelteryBlockEntity.this.fluidContainer::provideRender))
                            .append(Text.literal(GuiTextures.SMELTERY_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(SmelteryBlockEntity.this.getCachedState().getBlock().getName())
            );


            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }


            this.lastFluidUpdate = SmelteryBlockEntity.this.fluidContainer.updateId();
        }

        private void updateSmeltingProgress() {
            for (int y = 0; y < 3; y++) {
                var progress = new float[3];
                var enabled = new boolean[3];
                var color = new int[3];
                for (int x = 0; x < 3; x++) {
                    int i = y * 3 + x;
                    progress[x] = (float) SmelteryBlockEntity.this.progress[i] / SmelteryBlockEntity.this.progressEnd[i];
                    enabled[x] = SmelteryBlockEntity.this.progress[i] > -1 && !SmelteryBlockEntity.this.getStack(i).isEmpty();
                    color[x] = ColorHelper.lerp(progress[x], 0xFFFFFF, 0xFF2200);
                }
                this.setSlot(y * 9 + 4, new GuiElementBuilder(GuiTextures.LEFT_SHIFTED_3_BARS)
                        .hideTooltip()
                        .setCustomModelData(FloatList.of(progress), BooleanList.of(enabled), List.of(), IntList.of(color)));
            }
        }


        private float fuelProgress() {
            return SmelteryBlockEntity.this.fuelInitial > 0
                    ? MathHelper.clamp(SmelteryBlockEntity.this.fuelTicks / (float) SmelteryBlockEntity.this.fuelInitial, 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(SmelteryBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }

            if (SmelteryBlockEntity.this.fluidContainer.updateId() != this.lastFluidUpdate && delayTick < 0) {
                delayTick = 3;
            }
            if (this.delayTick-- == 0) {
                this.updateTitleAndFluid();
            }
            this.updateSmeltingProgress();

            var active = SmelteryBlockEntity.this.fuelTicks > 0;
            if (!this.active && active) {
                this.active = true;
                TriggerCriterion.trigger(this.player, FactoryTriggers.FUEL_STEAM_ENGINE);
            }
            this.setSlot(9 * 3 + 2, GuiTextures.FLAME.get(fuelProgress()));
            super.onTick();
        }
    }
}
