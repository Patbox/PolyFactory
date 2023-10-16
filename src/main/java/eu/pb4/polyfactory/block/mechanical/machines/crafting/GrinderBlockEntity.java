package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.base.LockableBlockEntity;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.RecipeCraftedCriterion;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class GrinderBlockEntity extends LockableBlockEntity implements MinimalSidedInventory {
    public static final int INPUT_SLOT = 0;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {1, 2, 3};
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(4, ItemStack.EMPTY);
    protected double process = 0;
    @Nullable
    protected RecipeEntry<GrindingRecipe> currentRecipe = null;
    @Nullable
    protected Item currentItem = null;
    private boolean active;

    public GrinderBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.GRINDER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, stacks);
        nbt.putDouble("Progress", this.process);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, this.stacks);
        this.process = nbt.getDouble("Progress");
        super.readNbt(nbt);
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(GrinderBlock.INPUT_FACING);
        return facing.getOpposite() == side ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot != INPUT_SLOT;
    }

    public void createGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (GrinderBlockEntity) t;

        var stack = self.getStack(0);
        if (stack.isEmpty()) {
            self.process = 0;
            self.active = false;
            return;
        }

        if (self.currentRecipe == null && self.currentItem != null && stack.isOf(self.currentItem)) {
            self.process = 0;
            self.active = false;
            return;
        }

        if (self.currentItem == null || !stack.isOf(self.currentItem)) {
            self.process = 0;
            self.currentItem = stack.getItem();
            self.currentRecipe = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.GRINDING, self, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                return;
            }
        }
        self.active = true;
        assert self.currentRecipe != null;

        if (self.process >= self.currentRecipe.value().grindTime()) {
            // Check space
            {
                var items = new ArrayList<ItemStack>();

                for (var out : self.currentRecipe.value().output()) {
                    for (int a = 0; a < out.roll(); a++) {
                        items.add(out.stack().copy());
                    }
                }

                var inv = new SimpleInventory(3);
                for (int i = 0; i < 3; i++) {
                    inv.setStack(i, self.getStack(i + 1).copy());
                }

                for (var item : items) {
                    FactoryUtil.tryInsertingInv(inv, item, null);

                    if (!item.isEmpty()) {
                        return;
                    }
                }
            }

            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayerEntity player) {
                Criteria.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), self.stacks.subList(0, 1));
            }

            var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().getSoundGroup(blockItem.getBlock().getDefaultState()).getBreakSound() : SoundEvents.BLOCK_STONE_BREAK;
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.6f, 0.5f);
            self.process = 0;
            stack.decrement(1);

            var items = new ArrayList<ItemStack>();

            for (var out : self.currentRecipe.value().output()) {
                for (int a = 0; a < out.roll(); a++) {
                    if (world.random.nextFloat() < out.chance()) {
                        items.add(out.stack().copy());
                    }
                }
            }

            for (var out : items) {
                for (int i = 1; i < 4; i++) {
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
            var d = Math.max(self.currentRecipe.value().optimalSpeed() - self.currentRecipe.value().minimumSpeed(), 1);

            var speed = Math.min(Math.max(Math.abs(RotationUser.getRotation((ServerWorld) world, pos).speed()) - self.currentRecipe.value().minimumSpeed(), 0), d) / d / 20;

            if (speed > 0) {
                if (world.getTime() % MathHelper.clamp(Math.round(1 / speed), 2, 5) == 0) {
                    ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()),
                            pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 0,
                            (Math.random() - 0.5) * 0.2, 0.02, (Math.random() - 0.5) * 0.2, 2);
                }
                if (world.getTime() % 20 == 0) {
                    var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().getSoundGroup(blockItem.getBlock().getDefaultState()).getHitSound() : SoundEvents.BLOCK_STONE_HIT;
                    world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.5f, 0.5f);
                }

                self.process += speed;
                self.markDirty();
            } else {
                ((ServerWorld) world).spawnParticles(ParticleTypes.POOF,
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0,
                        (Math.random() - 0.5) * 0.2, 0.02, (Math.random() - 0.5) * 0.2, 0.3);
            }
        }
    }

    public double getStress() {
        if (this.active) {
            return this.currentRecipe != null ? this.currentRecipe.value().optimalSpeed() * 0.6 : 4;
        }
        return 0;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.setTitle(GuiTextures.GRINDER.apply(GrinderBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.GRINDING));

            this.setSlotRedirect(4, new Slot(GrinderBlockEntity.this, 0, 0, 0));
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            this.setSlotRedirect(21, new FurnaceOutputSlot(player, GrinderBlockEntity.this, 1, 1, 0));
            this.setSlotRedirect(22, new FurnaceOutputSlot(player, GrinderBlockEntity.this, 2, 2, 0));
            this.setSlotRedirect(23, new FurnaceOutputSlot(player, GrinderBlockEntity.this, 3, 3, 0));
            this.open();
        }

        private float progress() {
            return GrinderBlockEntity.this.currentRecipe != null
                    ? (float) MathHelper.clamp(GrinderBlockEntity.this.process / GrinderBlockEntity.this.currentRecipe.value().grindTime(), 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(GrinderBlockEntity.this.pos)) > (18*18)) {
                this.close();
            }
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            super.onTick();
        }
    }
}
