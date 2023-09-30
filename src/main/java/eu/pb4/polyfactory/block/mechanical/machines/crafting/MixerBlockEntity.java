package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.machines.TallItemMachineBlockEntity;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.mixing.MixingRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.WrappingRecipeInputInventory;
import eu.pb4.polyfactory.util.movingitem.SimpleContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MixerBlockEntity extends TallItemMachineBlockEntity {
    public static final int OUTPUT_FIRST = 6;
    public static final int INPUT_FIRST = 0;
    public static final int SIZE = 9;
    private static final int[] OUTPUT_SLOTS = { 6, 7, 8 };
    private static final int[] INPUT_SLOTS = { 0, 1, 2, 3, 4, 5 };
    protected double process = 0;
    protected float temperature = 0;
    @Nullable
    protected RecipeEntry<MixingRecipe> currentRecipe = null;
    private boolean active;
    private final SimpleContainer[] containers = SimpleContainer.createArray(9, this::addMoving, this::removeMoving);
    private MixerBlock.Model model;
    private boolean inventoryChanged = false;
    private RecipeInputInventory recipeInputProvider;

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
    protected void writeNbt(NbtCompound nbt) {
        this.writeInventoryNbt(nbt);
        nbt.putDouble("Progress", this.process);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.readInventoryNbt(nbt);
        this.process = nbt.getDouble("Progress");
        super.readNbt(nbt);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(GrinderBlock.INPUT_FACING);
        return facing.getOpposite() == side ? OUTPUT_SLOTS : INPUT_SLOTS;
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

        var belowBlock = world.getBlockState(pos.down());

        if (belowBlock.isIn(BlockTags.CAMPFIRES)) {
            self.temperature = 0.5f;
        } else if (belowBlock.isIn(BlockTags.FIRE)) {
            self.temperature = 0.6f;
        } else if (belowBlock.isOf(Blocks.LAVA)) {
            self.temperature = 0.8f;
        } else if (belowBlock.isOf(Blocks.TORCH)) {
            self.temperature = 0.2f;
        } else if (belowBlock.isOf(Blocks.TORCHFLOWER)) {
            self.temperature = 0.1f;
        } else {
            self.temperature = 0;
        }

        if (self.isInputEmpty()) {
            self.process = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        if (self.currentRecipe == null && !self.inventoryChanged) {
            self.process = 0;
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        if (self.inventoryChanged && (self.currentRecipe == null || !self.currentRecipe.value().matches(self, world))) {
            self.process = 0;
            self.currentRecipe = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.MIXER, self, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.model.setActive(false);
                self.model.tick();
                self.inventoryChanged = false;
                return;
            }
        }
        self.inventoryChanged = false;


        if (self.temperature < self.currentRecipe.value().minimumTemperature() || self.temperature > self.currentRecipe.value().maxTemperature()) {
            self.active = false;
            self.model.setActive(false);
            self.model.tick();
            return;
        }

        self.active = true;
        self.model.setActive(true);
        var fullSpeed = RotationUser.getRotation((ServerWorld) world, pos.up()).speed();
        self.model.rotate((float) fullSpeed);
        self.model.tick();

        if (self.process >= self.currentRecipe.value().time()) {
            var currentOutput = self.getStack(OUTPUT_FIRST);
            var output = self.currentRecipe.value().craft(self, world.getRegistryManager());
            {
                if (!currentOutput.isEmpty() && (!ItemStack.canCombine(currentOutput, output) || output.getCount() + currentOutput.getCount() > output.getMaxCount())) {
                    return;
                }
            }
            self.process = 0;

            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayerEntity player) {
                Criteria.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), self.asRecipeInputProvider().getInputStacks());
                TriggerCriterion.trigger(player, FactoryTriggers.MIXER_CRAFTS);
            }

            self.currentRecipe.value().applyRecipeUse(self, world);

            if (currentOutput.isEmpty()) {
                self.setStack(OUTPUT_FIRST, output);
            } else {
                currentOutput.increment(output.getCount());
            }

            for (var remainder : self.currentRecipe.value().remainders()) {
                if (remainder.isEmpty()) {
                    continue;
                }

                for (int i = OUTPUT_FIRST; i < SIZE; i++) {
                    var slot = self.getStack(i);
                    if (slot.isEmpty()) {
                        self.setStack(i, remainder.copyAndEmpty());
                    } else if (ItemStack.canCombine(slot, remainder)) {
                        var count = Math.max(remainder.getCount(), slot.getMaxCount() - slot.getCount());
                        remainder.decrement(count);
                        slot.increment(count);

                        if (remainder.isEmpty()) {
                            break;
                        }
                    }
                }

                if (!remainder.isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, remainder);
                }
            }

            self.markDirty();
        } else {
            var d = Math.max(self.currentRecipe.value().optimalSpeed() - self.currentRecipe.value().minimumSpeed(), 1);
            var speed = Math.min(Math.max(Math.abs(fullSpeed) - self.currentRecipe.value().minimumSpeed(), 0), d) / d / 20;
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
            }
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
            return this.currentRecipe != null ? this.currentRecipe.value().optimalSpeed() * 0.6 : 4;
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
    public @Nullable BaseModel getModel() {
        return this.model;
    }

    public RecipeInputInventory asRecipeInputProvider() {
        if (this.recipeInputProvider == null) {
            this.recipeInputProvider = WrappingRecipeInputInventory.of(this, INPUT_FIRST, OUTPUT_FIRST, 2, 3);
        }
        return this.recipeInputProvider;
    }

    public float temperature() {
        return this.temperature;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, false);
            this.setTitle(GuiTextures.MIXER.apply(MixerBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.MIXER));

            this.setSlotRedirect(2, new Slot(MixerBlockEntity.this, 0, 0, 0));
            this.setSlotRedirect(3, new Slot(MixerBlockEntity.this, 1, 1, 0));
            this.setSlotRedirect(2 + 9, new Slot(MixerBlockEntity.this, 2, 2, 0));
            this.setSlotRedirect(3 + 9, new Slot(MixerBlockEntity.this, 3, 3, 0));
            this.setSlotRedirect(2 + 18, new Slot(MixerBlockEntity.this, 4, 4, 0));
            this.setSlotRedirect(3 + 18, new Slot(MixerBlockEntity.this, 5, 5, 0));
            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            this.setSlot(4 + 9 + 9, GuiTextures.FLAME_OFFSET_RIGHT.get(MathHelper.clamp(MixerBlockEntity.this.temperature, 0, 1)));
            this.setSlotRedirect(6, new FurnaceOutputSlot(player, MixerBlockEntity.this, 6, 3, 0));
            this.setSlotRedirect(6 + 9, new FurnaceOutputSlot(player, MixerBlockEntity.this, 7, 3, 0));
            this.setSlotRedirect(6 + 18, new FurnaceOutputSlot(player, MixerBlockEntity.this, 8, 3, 0));
            this.open();
        }

        private float progress() {
            return MixerBlockEntity.this.currentRecipe != null
                    ? (float) MathHelper.clamp(MixerBlockEntity.this.process / MixerBlockEntity.this.currentRecipe.value().time(), 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(MixerBlockEntity.this.pos)) > (18*18)) {
                this.close();
            }
            this.setSlot(4 + 9, GuiTextures.PROGRESS_HORIZONTAL_OFFSET_RIGHT.get(progress()));
            this.setSlot(4 + 9 + 9, GuiTextures.FLAME_OFFSET_RIGHT.get(MathHelper.clamp(MixerBlockEntity.this.temperature, 0, 1)));
            super.onTick();
        }
    }
}
