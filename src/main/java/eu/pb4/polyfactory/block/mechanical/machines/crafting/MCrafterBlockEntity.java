package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.other.ItemOutputBufferBlock;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.block.other.OutputContainerOwner;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.CrafterLikeInsertContainer;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.polyfactory.util.inventory.SubContainer;
import eu.pb4.polyfactory.util.inventory.WrappingInputRecipeInput;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class MCrafterBlockEntity extends LockableBlockEntity implements MachineInfoProvider, MinimalSidedContainer, CrafterLikeInsertContainer, CraftingContainer, OutputContainerOwner {
    private static final int[] INPUT_SLOTS = IntStream.range(0, 9).toArray();
    private static final int[] OUTPUT_SLOTS = IntStream.range(9, 9 + 9).toArray();
    private static final SoundEvent CRAFT_SOUND_EVENT = SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:block.crafter.craft"));
    //private static final int[] OUTPUT_SLOTS = {1, 2, 3};
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(9 + 9, ItemStack.EMPTY);

    private BitSet lockedSlots = new BitSet(9);
    protected double process = 0;
    @Nullable
    protected RecipeHolder<CraftingRecipe> currentRecipe = null;
    private boolean active;
    private boolean itemsDirty;
    private final CraftingContainer recipeInputProvider = WrappingInputRecipeInput.of(this, 0, 9, 3, 3);
    @Nullable
    private Component state;
    private ActiveMode activeMode = ActiveMode.FILLED;

    private final Container outputContainer = new SubContainer(this, 9);

    public MCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CRAFTER, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.stacks);
        view.putDouble("progress", this.process);
        view.store("locked_slots", ExtraCodecs.BIT_SET, this.lockedSlots);
        view.putString("active_mode", this.activeMode.getSerializedName());
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, this.stacks);
        this.process = view.getDoubleOr("progress", 0);
        this.lockedSlots = view.read("locked_slots", ExtraCodecs.BIT_SET).orElseGet(() -> new BitSet(9));
        this.activeMode = view.read("active_mode", ActiveMode.CODEC).orElse(ActiveMode.FILLED);

        super.loadAdditional(view);
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        var facing = this.getBlockState().getValue(MCrafterBlock.FACING);
        return facing == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        if (this.lockedSlots.get(slot)) {
            return false;
        }

        if (dir != null && (dir == Direction.UP || dir.getOpposite() == this.getBlockState().getValue(MCrafterBlock.FACING))) {
            return getLeastPopulatedInputSlot(stack) == slot;
        }

        return slot < 9;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot >= 9 && (dir == this.getBlockState().getValue(MCrafterBlock.FACING) || dir == Direction.DOWN)
                || slot < 9 && this.getBlockState().getValue(MCrafterBlock.FACING).getClockWise().getAxis() == dir.getAxis();
    }

    public void createGui(ServerPlayer player) {
        new Gui(player);
    }

    @Override
    public void setChanged() {
        this.itemsDirty = true;
        super.setChanged();
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (MCrafterBlockEntity) t;
        self.state = null;
        if (self.activeMode.prevent(self)) {
            self.process = 0;
            self.active = false;
            return;
        }

        if (self.currentRecipe == null && !self.itemsDirty) {
            self.state = INCORRECT_ITEMS_TEXT;
            self.active = false;
            return;
        }

        var input = self.recipeInputProvider.asCraftInput();
        if (self.currentRecipe == null || (self.itemsDirty && !self.currentRecipe.value().matches(input, world))) {
            self.process = 0;
            self.currentRecipe = ((ServerLevel) world).recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, world).orElse(null);
            self.itemsDirty = false;
            if (self.currentRecipe == null) {
                self.state = INCORRECT_ITEMS_TEXT;
                self.active = false;
                return;
            }
        }
        self.active = true;
        assert self.currentRecipe != null;

        if (self.process >= 8) {
            var outputContainer = self.getOutputContainer();
            // Check space
            var output = self.currentRecipe.value().assemble(input, world.registryAccess());
            var remainder = self.currentRecipe.value().getRemainingItems(input);

            {
                var items = new ArrayList<ItemStack>();
                items.add(output.copy());
                for (var stack : remainder) {
                    items.add(stack.copy());
                }

                var inv = new SimpleContainer(outputContainer.getContainerSize());
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

            if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayer player) {
                TriggerCriterion.trigger(player, FactoryTriggers.CRAFTER_CRAFTS);
                if (output.is(FactoryItems.CRAFTER)) {
                    TriggerCriterion.trigger(player, FactoryTriggers.CRAFTER_CRAFTS_CRAFTER);
                }
                CriteriaTriggers.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), self.stacks.subList(0, 9));
            }

            for (int i = 0; i < 9; i++) {
                var stack = self.getItem(i);
                if (!stack.isEmpty()) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        self.setItem(i, ItemStack.EMPTY);
                    }
                }
            }

            world.playSound(null, pos, CRAFT_SOUND_EVENT, SoundSource.BLOCKS, 0.6f, 0.5f);
            self.process = 0;

            var items = new ArrayList<ItemStack>();
            items.add(output);
            items.addAll(remainder);



            for (var out : items) {
                FactoryUtil.tryInsertingRegular(outputContainer, out);
            }

            self.setChanged();
        } else {
            var rot = RotationUser.getRotation(world, pos);
            var speed = Math.min(rot.speed() / 40, 1);

            if (speed > 0.05) {
                //if (world.getTime() % MathHelper.clamp(Math.round(1 / speed), 2, 5) == 0) {
                //    ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                //            pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 0,
                //            (Math.random() - 0.5) * 0.2, 0.02, (Math.random() - 0.5) * 0.2, 2);
                //}
                //if (world.getTime() % 20 == 0) {
                //    var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().getSoundGroup(blockItem.getBlock().getDefaultState()).getHitSound() : SoundEvents.BLOCK_STONE_HIT;
                //    world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.5f, 0.5f);
                //}

                self.process += speed;
                self.setChanged();
                return;
            } else if (world.getGameTime() % 5 == 0) {
                ((ServerLevel) world).sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0,
                        (Math.random() - 0.5) * 0.2, 0.04, (Math.random() - 0.5) * 0.2, 0.3);
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
        return ItemOutputBufferBlock.getOutputContainer(this.outputContainer, this.level, this.getBlockPos(), this.getBlockState().getValue(MCrafterBlock.FACING));
    }

    @Override
    public boolean isOutputConnectedTo(Direction dir) {
        return this.getBlockState().getValue(MCrafterBlock.FACING) == dir;
    }

    private boolean isInputNotFull() {
        int full = 0;
        int locked = 0;

        for (int i = 0; i < 9; i++) {
            if (this.lockedSlots.get(i)) {
                locked++;
                full++;
            } else if (!this.getItem(i).isEmpty()) {
                full++;
            }
        }
        return full < 9 || locked == 9;
    }

    public double getStress() {
        if (this.active) {
            return this.currentRecipe != null ? 6 : 3;
        }
        return 0;
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public List<ItemStack> getItems() {
        return List.copyOf(this.stacks.subList(0, 9));
    }

    @Override
    public void fillStackedContents(StackedItemContents finder) {
        for (int i = 0; i < 9; i++) {
            finder.accountStack(this.getItem(i));
        }
    }

    @Override
    public @Nullable Component getCurrentState() {
        return this.state;
    }

    @Override
    public boolean isSlotLocked(int i) {
        return this.lockedSlots.get(i);
    }

    @Override
    public int inputSize() {
        return 9;
    }

    public ActiveMode getActiveMode() {
        return this.activeMode;
    }

    public void setActiveMode(ActiveMode mode) {
        this.activeMode = mode;
        this.setChanged();
    }

    private class Gui extends SimpleGui {
        private final BitSet lockedSlots;

        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.lockedSlots = (BitSet) MCrafterBlockEntity.this.lockedSlots.clone();
            this.setTitle(GuiTextures.CRAFTER.apply(MCrafterBlockEntity.this.getBlockState().getBlock().getName()));
            this.setSlot(9, PolydexCompat.getButton(RecipeType.CRAFTING));

            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    this.setLockableSlot(x + y * 9 + 1, x + y * 3, x, y);
                    this.setSlotRedirect(x + y * 9 + 5, new FurnaceResultSlot(player, MCrafterBlockEntity.this, 9 + x + y * 3, x, y + 3));
                }
            }

            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL.get(progress()));
            this.open();
        }

        private void setLockableSlot(int uiIndex, int slot, int x, int y) {
            if (this.lockedSlots.get(slot)) {
                this.setSlot(uiIndex, GuiTextures.LOCKED_SLOT.get().hideTooltip());
            } else {
                this.setSlotRedirect(uiIndex, new Slot(MCrafterBlockEntity.this, x + y * 3, x, y));
            }
        }

        @Override
        public boolean onAnyClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action) {
            var x = index % 9 - 1;
            var y = index / 9;
            if ((type == ClickType.MOUSE_LEFT || type == ClickType.MOUSE_RIGHT) && x >= 0 && y >= 0 && x < 3 && y < 3) {
                var slot = x + y * 3;
                var locked = MCrafterBlockEntity.this.lockedSlots.get(slot);
                if (locked) {
                    MCrafterBlockEntity.this.lockedSlots.set(slot, false);
                    FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.4f, 1);
                    return true;
                } else if (MCrafterBlockEntity.this.getItem(slot).isEmpty() && this.getPlayer().containerMenu.getCarried().isEmpty()) {
                    MCrafterBlockEntity.this.lockedSlots.set(slot, true);
                    FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.4f, 0.75f);
                    return true;
                }
            }


            return super.onAnyClick(index, type, action);
        }

        private float progress() {
            return MCrafterBlockEntity.this.currentRecipe != null
                    ? (float) Mth.clamp(MCrafterBlockEntity.this.process / 8, 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(MCrafterBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    var i = x + y * 3;
                    if (this.lockedSlots.get(i) != MCrafterBlockEntity.this.lockedSlots.get(i)) {
                        this.lockedSlots.flip(i);
                        this.setLockableSlot(x + y * 9 + 1, i, x, y);
                    }
                }
            }

            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL.get(progress()));
            super.onTick();
        }
    }

    public enum ActiveMode implements StringRepresentable {
        FILLED("filled", MCrafterBlockEntity::isInputNotFull),
        ALWAYS("always", be -> false),
        POWERED("powered", be -> !be.getBlockState().getValue(MCrafterBlock.POWERED)),
        NOT_POWERED("not_powered", be -> be.getBlockState().getValue(MCrafterBlock.POWERED));

        public static final Codec<ActiveMode> CODEC = StringRepresentable.fromEnum(ActiveMode::values);

        private final String name;
        private final Predicate<MCrafterBlockEntity> prevent;

        ActiveMode(String id, Predicate<MCrafterBlockEntity> prevent) {
            this.name = id;
            this.prevent = prevent;
        }

        public boolean prevent(MCrafterBlockEntity be) {
            return this.prevent.test(be);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Component asText() {
            return Component.translatable("item.polyfactory.wrench.action.mechanical_crafter.active_mode." + getSerializedName());
        }
    }
}
