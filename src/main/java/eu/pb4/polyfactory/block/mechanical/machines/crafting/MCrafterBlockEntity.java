package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.base.LockableBlockEntity;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.polyfactory.util.inventory.WrappingRecipeInputInventory;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

public class MCrafterBlockEntity extends LockableBlockEntity implements MinimalSidedInventory, RecipeInputInventory {
    private static final int[] INPUT_SLOTS = IntStream.range(0, 9).toArray();
    private static final int[] OUTPUT_SLOTS = IntStream.range(9, 9+9).toArray();
    private static final SoundEvent CRAFT_SOUND_EVENT = SoundEvent.of(new Identifier("minecraft:block.crafter.craft"));
    //private static final int[] OUTPUT_SLOTS = {1, 2, 3};
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(9+9, ItemStack.EMPTY);

    private BitSet lockedSlots = new BitSet(9);
    protected double process = 0;
    @Nullable
    protected RecipeEntry<CraftingRecipe> currentRecipe = null;
    private boolean active;
    private boolean itemsDirty;
    private final RecipeInputInventory recipeInputProvider = WrappingRecipeInputInventory.of(this, 0, 9, 3, 3);

    public MCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CRAFTER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, stacks);
        nbt.putDouble("progress", this.process);
        nbt.putByteArray("locked_slots", this.lockedSlots.toByteArray());
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, this.stacks);
        this.process = nbt.getDouble("progress");
        this.lockedSlots = BitSet.valueOf(nbt.getByteArray("locked_slots"));
        super.readNbt(nbt);
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(MCrafterBlock.FACING);
        return facing == side ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (this.lockedSlots.get(slot)) {
            return false;
        }

        if (dir != null && (dir == Direction.UP || dir.getOpposite() == this.getCachedState().get(MCrafterBlock.FACING))) {
            return getLeastPopulatedInputSlot(stack) == slot;
        }

        return slot < 9;
    }

    private int getLeastPopulatedInputSlot(ItemStack stack) {
        int slot = -1;
        int count = 9999;

        for (int i = 0; i < 9; i++) {
            if (this.lockedSlots.get(i)) {
                continue;
            }
            var cur = this.getStack(i);

            if (cur.isEmpty()) {
                return i;
            }

            if (ItemStack.canCombine(cur, stack)) {
                if (count > cur.getCount() && cur.getCount() < cur.getMaxCount()) {
                    count = cur.getCount();
                    slot = i;
                }
            }
        }

        return slot;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot >= 9 && dir == this.getCachedState().get(MCrafterBlock.FACING);
    }

    public void createGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    @Override
    public void markDirty() {
        this.itemsDirty = true;
        super.markDirty();
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (MCrafterBlockEntity) t;

        if (self.isInputNotFull()) {
            self.process = 0;
            self.active = false;
            return;
        }

        if (self.currentRecipe == null || (self.itemsDirty && !self.currentRecipe.value().matches(self.recipeInputProvider, world))) {
            self.process = 0;
            self.currentRecipe = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, self.recipeInputProvider, world).orElse(null);
            self.itemsDirty = false;
            if (self.currentRecipe == null) {
                self.active = false;
                return;
            }
        }
        self.active = true;
        assert self.currentRecipe != null;

        if (self.process >= 8) {
            // Check space
            var output = self.currentRecipe.value().craft(self.recipeInputProvider, world.getRegistryManager());
            var remainder = self.currentRecipe.value().getRemainder(self.recipeInputProvider);

            {
                var items = new ArrayList<ItemStack>();
                items.add(output.copy());
                items.addAll(remainder);

                var inv = new SimpleInventory(9);
                for (int i = 0; i < 9; i++) {
                    inv.setStack(i, self.getStack(i + 9).copy());
                }

                for (var item : items) {
                    FactoryUtil.tryInsertingInv(inv, item, null);

                    if (!item.isEmpty()) {
                        return;
                    }
                }
            }

            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayerEntity player) {
                Criteria.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), self.stacks.subList(0, 9));
            }

            for (int i = 0; i < 9; i++) {
                var stack = self.getStack(i);
                if (!stack.isEmpty()) {
                    stack.decrement(1);
                    if (stack.isEmpty()) {
                        self.setStack(i, ItemStack.EMPTY);
                    }
                }
            }

            world.playSound(null, pos, CRAFT_SOUND_EVENT, SoundCategory.BLOCKS, 0.6f, 0.5f);
            self.process = 0;

            var items = new ArrayList<ItemStack>();
            items.add(output);
            items.addAll(remainder);

            for (var out : items) {
                for (int i = 9; i < 9 + 9; i++) {
                    var c = self.getStack(i);
                    if (c.isEmpty()) {
                        self.setStack(i, out);
                        break;
                    } else if (ItemStack.canCombine(c, out)) {
                        var count = Math.min((c.getMaxCount() - c.getCount()), out.getCount());
                        c.increment(count);
                        out.decrement(count);
                    }

                    if (out.isEmpty()) {
                        break;
                    }
                }
            }

            self.markDirty();
        } else {

            var speed = Math.min(RotationUser.getRotation(world, pos).speed() / 40, 1);

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
                self.markDirty();
            } else {
                ((ServerWorld) world).spawnParticles(ParticleTypes.POOF,
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0,
                        (Math.random() - 0.5) * 0.2, 0.02, (Math.random() - 0.5) * 0.2, 0.3);
            }
        }
    }

    private boolean isInputNotFull() {
        int full = 0;
        int locked = 0;

        for (int i = 0; i < 9; i++) {
            if (this.lockedSlots.get(i)) {
                locked++;
                full++;
            } else if (!this.getStack(i).isEmpty()) {
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
    public List<ItemStack> getInputStacks() {
        return List.copyOf(this.stacks.subList(0, 9));
    }

    @Override
    public void provideRecipeInputs(RecipeMatcher finder) {
        for (int i = 0; i < 9; i++) {
            finder.addUnenchantedInput(this.getStack(i));
        }
    }

    private class Gui extends SimpleGui {
        private final BitSet lockedSlots;
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.lockedSlots = (BitSet) MCrafterBlockEntity.this.lockedSlots.clone();
            this.setTitle(GuiTextures.CRAFTER.apply(MCrafterBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlot(9, PolydexCompat.getButton(RecipeType.CRAFTING));

            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    this.setLockableSlot(x + y * 9 + 1, x + y * 3, x, y);
                    this.setSlotRedirect(x + y * 9 + 5, new FurnaceOutputSlot(player, MCrafterBlockEntity.this, 9 + x + y * 3, x, y + 3));
                }
            }

            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL.get(progress()));
            this.open();
        }

        private void setLockableSlot(int uiIndex, int slot, int x, int y) {
            if (this.lockedSlots.get(slot)) {
                this.setSlot(uiIndex, GuiTextures.LOCKED_SLOT.get());
            } else {
                this.setSlotRedirect(uiIndex, new Slot(MCrafterBlockEntity.this, x + y * 3, x, y));
            }
        }

        @Override
        public boolean onAnyClick(int index, ClickType type, SlotActionType action) {
            var x = index % 9 - 1;
            var y = index / 9;
            if (x >= 0 && y >= 0 && x < 3 && y < 3) {
                var slot = x + y * 3;
                var locked = MCrafterBlockEntity.this.lockedSlots.get(slot);
                if (locked) {
                    MCrafterBlockEntity.this.lockedSlots.set(slot, false);
                    GuiUtils.playClickSound(player);
                    return true;
                } else if (MCrafterBlockEntity.this.getStack(slot).isEmpty() && this.getPlayer().currentScreenHandler.getCursorStack().isEmpty()) {
                    MCrafterBlockEntity.this.lockedSlots.set(slot, true);
                    GuiUtils.playClickSound(player);
                    return true;
                }
            }


            return super.onAnyClick(index, type, action);
        }

        private float progress() {
            return MCrafterBlockEntity.this.currentRecipe != null
                    ? (float) MathHelper.clamp(MCrafterBlockEntity.this.process / 8, 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(MCrafterBlockEntity.this.pos)) > (18*18)) {
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
}
