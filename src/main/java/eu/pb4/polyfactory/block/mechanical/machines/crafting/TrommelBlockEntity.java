package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.other.ItemOutputBufferBlock;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.block.other.OutputContainerOwner;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.trommel.TrommelRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalWorldlyContainer;
import eu.pb4.polyfactory.util.inventory.RedirectingWorldlyContainer;
import eu.pb4.polyfactory.util.inventory.SubContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TrommelBlockEntity extends LockableBlockEntity implements MinimalWorldlyContainer, MachineInfoProvider, OutputContainerOwner, BlockEntityExtraListener {
    public static final int INPUT_SLOT = 0;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {1, 2, 3, 4, 5};
    final WorldlyContainer worldInputContainer = RedirectingWorldlyContainer.inputOnly(this);
    final WorldlyContainer worldOutputContainer = RedirectingWorldlyContainer.outputOnly(this);
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(6, ItemStack.EMPTY);
    private final Container outputContainer = new SubContainer(this, 1);
    protected double process = 0;
    @Nullable
    protected RecipeHolder<TrommelRecipe> currentRecipe = null;
    @Nullable
    protected Item currentItem = null;
    private TrommelBlock.Model model;
    private boolean active;
    private double speedScale;
    private Component state = null;


    public TrommelBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.TROMMEL, pos, state);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (TrommelBlockEntity) t;

        var stack = self.getItem(0);
        self.state = null;
        self.model.setItem(stack);

        if (stack.isEmpty()) {
            self.process = 0;
            self.model.setProgress(0);
            self.active = false;
            self.model.setActive(false);
            self.speedScale = 0;
            return;
        }

        if (self.currentRecipe == null && self.currentItem != null && stack.is(self.currentItem)) {
            self.process = 0;
            self.model.setProgress(0);
            self.active = false;
            self.model.setActive(false);
            self.speedScale = 0;
            self.state = INCORRECT_ITEMS_TEXT;
            return;
        }
        var input = new SingleRecipeInput(self.stacks.getFirst().copy());
        if (self.currentItem == null || !stack.is(self.currentItem)) {
            self.process = 0;
            self.model.setProgress(0);
            self.speedScale = 0;
            self.currentItem = stack.getItem();
            self.currentRecipe = ((ServerLevel) world).recipeAccess().getRecipeFor(FactoryRecipeTypes.TROMMEL, input, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.model.setActive(false);
                self.state = INCORRECT_ITEMS_TEXT;
                return;
            }
        }
        self.active = true;
        self.model.setActive(true);

        assert self.currentRecipe != null;

        var requiredTime = self.currentRecipe.value().time(input);

        self.model.setProgress(Mth.clamp((float) (self.process / requiredTime), 0, 1));

        if (self.process >= requiredTime) {
            var output = self.getOutputContainer();
            // Check space
            {
                var inv = new SimpleContainer(output.getContainerSize());
                for (int i = 0; i < output.getContainerSize(); i++) {
                    inv.setItem(i, output.getItem(i).copy());
                }

                for (var item : self.currentRecipe.value().output(input, null)) {
                    FactoryUtil.tryInsertingInv(inv, item, null);

                    if (!item.isEmpty()) {
                        self.state = OUTPUT_FULL_TEXT;
                        return;
                    }
                }
            }

            var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState().getSoundType().getBreakSound() : SoundEvents.STONE_BREAK;
            world.playSound(null, pos, sound, SoundSource.BLOCKS, 0.6f, 0.25f);
            self.process = 0;
            stack.shrink(1);

            int successCount = 0;
            for (var out : self.currentRecipe.value().output(input, world.getRandom())) {
                FactoryUtil.tryInsertingRegular(output, out.copy());
                successCount++;
            }

            if (successCount >= 1) {
                if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayer player) {
                    CriteriaTriggers.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), self.stacks.subList(0, 1));
                    TriggerCriterion.trigger(player, FactoryTriggers.TROMMEL_SUCCESS);
                }
            }

            self.setChanged();
        } else {
            var d = Math.max(self.currentRecipe.value().optimalSpeed(input) - self.currentRecipe.value().minimumSpeed(input), 1);
            var rot = RotationUser.getRotation(world, pos);
            var speed = Math.min(Math.max(Math.abs(rot.speed()) - self.currentRecipe.value().minimumSpeed(input), 0), d) / d / 20;
            self.speedScale = speed;
            if (speed > 0) {
                if (world.getGameTime() % 15 == 0) {
                    var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState().getSoundType().getHitSound() : SoundEvents.STONE_HIT;
                    world.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5f, 0.75f);
                }

                self.process += speed;
                self.setChanged();
                return;
            } else if (world.getGameTime() % 8 == 0) {
                var dir = state.getValue(TrommelBlock.FACING).getAxis().getPositive();
                ((ServerLevel) world).sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5 + 0.5 * dir.getStepX(), pos.getY() + 0.75, pos.getZ() + 0.5 + 0.5 * dir.getStepZ(), 3,
                        0.25 + 0.25 * dir.getStepX(), 0.2, 0.25 + 0.25 * dir.getStepZ(), 0.01, 0.03, 0.01);
            }

            self.state = rot.getStateTextOrElse(rot.hasNoActiveProviders() ? TOO_SLOW_TEXT : TOO_SLOW_DISCONNECTED_TEXT);
        }
    }

    public static double getActiveStress(double minimalSpeed, double optimalSpeed, double speedScale) {
        return Mth.clamp(optimalSpeed * speedScale,
                minimalSpeed,
                optimalSpeed
        ) * 0.7;
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.stacks);
        view.putDouble("Progress", this.process);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, this.stacks);
        this.process = view.getDoubleOr("Progress", 0);
        this.currentItem = null;
        super.loadAdditional(view);
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        var facing = this.getBlockState().getValue(TrommelBlock.FACING);
        return facing == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot != INPUT_SLOT;
    }

    public void createGui(ServerPlayer player) {
        new Gui(player);
    }

    public double getStress() {
        if (this.active) {
            var input = new SingleRecipeInput(this.stacks.getFirst());

            return this.currentRecipe != null ?
                    getActiveStress(
                            this.currentRecipe.value().minimumSpeed(input),
                            this.currentRecipe.value().optimalSpeed(input),
                            this.speedScale) : 1;
        }
        return 0;
    }

    @Override
    public @Nullable Component getCurrentState() {
        return this.state;
    }

    @Override
    public Container getOwnOutputContainer() {
        return this.outputContainer;
    }

    @Override
    public Container getOutputContainer() {
        return ItemOutputBufferBlock.getOutputContainer(this.outputContainer, this.level, this.getBlockPos(), this.getBlockState().getValue(TrommelBlock.FACING));
    }

    @Override
    public boolean isOutputConnectedTo(Direction dir) {
        return this.getBlockState().getValue(TrommelBlock.FACING) == dir;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        if (BlockAwareAttachment.get(chunk, this.getBlockPos()) instanceof BlockAwareAttachment attachment && attachment.holder() instanceof TrommelBlock.Model model) {
            this.model = model;
        }
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.setTitle(GuiTextures.GRINDER.apply(TrommelBlockEntity.this.getName()));
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.TROMMEL));

            this.setSlot(4, new Slot(TrommelBlockEntity.this, 0, 0, 0));
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            this.setSlot(20, new FurnaceResultSlot(player, TrommelBlockEntity.this, 1, 1, 0));
            this.setSlot(21, new FurnaceResultSlot(player, TrommelBlockEntity.this, 2, 2, 0));
            this.setSlot(22, new FurnaceResultSlot(player, TrommelBlockEntity.this, 3, 3, 0));
            this.setSlot(23, new FurnaceResultSlot(player, TrommelBlockEntity.this, 4, 4, 0));
            this.setSlot(24, new FurnaceResultSlot(player, TrommelBlockEntity.this, 5, 5, 0));
            this.open();
        }

        private float progress() {
            return TrommelBlockEntity.this.currentRecipe != null
                    ? (float) Mth.clamp(TrommelBlockEntity.this.process / TrommelBlockEntity.this.currentRecipe.value().time(new SingleRecipeInput(TrommelBlockEntity.this.stacks.getFirst())), 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(TrommelBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            super.onTick();
        }
    }
}
