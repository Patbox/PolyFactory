package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.GrindingRecipe;
import eu.pb4.polyfactory.recipe.input.GrindingInput;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.sgui.api.gui.SimpleGui;
import org.jetbrains.annotations.Nullable;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class GrinderBlockEntity extends LockableBlockEntity implements MinimalSidedContainer, MachineInfoProvider {
    public static final int INPUT_SLOT = 0;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {1, 2, 3};
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(4, ItemStack.EMPTY);
    protected double process = 0;
    @Nullable
    protected RecipeHolder<GrindingRecipe> currentRecipe = null;
    @Nullable
    protected Item currentItem = null;
    private boolean active;
    private double speedScale;
    private Component state = null;

    public GrinderBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.GRINDER, pos, state);
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
        var facing = this.getBlockState().getValue(GrinderBlock.INPUT_FACING);
        return facing.getOpposite() == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
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

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (GrinderBlockEntity) t;

        var stack = self.getItem(0);
        self.state = null;
        if (stack.isEmpty()) {
            self.process = 0;
            self.active = false;
            self.speedScale = 0;
            return;
        }

        if (self.currentRecipe == null && self.currentItem != null && stack.is(self.currentItem)) {
            self.process = 0;
            self.active = false;
            self.speedScale = 0;
            self.state = INCORRECT_ITEMS_TEXT;
            return;
        }
        var input = new GrindingInput(self.stacks.getFirst().copy());
        if (self.currentItem == null || !stack.is(self.currentItem)) {
            self.process = 0;
            self.speedScale = 0;
            self.currentItem = stack.getItem();
            self.currentRecipe = ((ServerLevel) world).recipeAccess().getRecipeFor(FactoryRecipeTypes.GRINDING, input, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.state = INCORRECT_ITEMS_TEXT;
                return;
            }
        }
        self.active = true;
        assert self.currentRecipe != null;

        if (self.process >= self.currentRecipe.value().grindTime(input)) {
            // Check space
            {
                var inv = new SimpleContainer(3);
                for (int i = 0; i < 3; i++) {
                    inv.setItem(i, self.getItem(i + 1).copy());
                }

                for (var item : self.currentRecipe.value().output(input, world.registryAccess(), null)) {
                    FactoryUtil.tryInsertingInv(inv, item, null);

                    if (!item.isEmpty()) {
                        self.state = OUTPUT_FULL_TEXT;
                        return;
                    }
                }
            }

            if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayer player) {
                CriteriaTriggers.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), self.stacks.subList(0, 1));
            }

            var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState().getSoundType().getBreakSound() : SoundEvents.STONE_BREAK;
            world.playSound(null, pos, sound, SoundSource.BLOCKS, 0.6f, 0.5f);
            self.process = 0;
            stack.shrink(1);

            ;

            for (var out : self.currentRecipe.value().output(input, world.registryAccess(), world.random)) {
                FactoryUtil.insertBetween(self, 1, 4, out.copy());
            }

            self.setChanged();
        } else {
            var d = Math.max(self.currentRecipe.value().optimalSpeed(input) - self.currentRecipe.value().minimumSpeed(input), 1);
            var rot = RotationUser.getRotation((ServerLevel) world, pos);
            var speed = Math.min(Math.max(Math.abs(rot.speed()) - self.currentRecipe.value().minimumSpeed(input), 0), d) / d / 20;
            self.speedScale = speed;
            if (speed > 0) {
                if (world.getGameTime() % Mth.clamp(Math.round(1 / speed), 2, 5) == 0) {
                    ((ServerLevel) world).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.copy()),
                            pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 0,
                            (Math.random() - 0.5) * 0.2, 0.02, (Math.random() - 0.5) * 0.2, 2);
                }
                if (world.getGameTime() % 20 == 0) {
                    var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState().getSoundType().getHitSound() : SoundEvents.STONE_HIT;
                    world.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5f, 0.5f);
                }

                self.process += speed;
                self.setChanged();
                return;
            } else if (world.getGameTime() % 5 == 0) {
                ((ServerLevel) world).sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 0,
                        (Math.random() - 0.5) * 0.2, 0.04, (Math.random() - 0.5) * 0.2, 0.3);
            }

            self.state = rot.getStateTextOrElse(TOO_SLOW_TEXT);
        }
    }

    public double getStress() {
        if (this.active) {
            var input = new GrindingInput(this.stacks.getFirst());

            return this.currentRecipe != null ?
                    Mth.clamp(this.currentRecipe.value().optimalSpeed(input) * 0.7 * this.speedScale,
                            this.currentRecipe.value().minimumSpeed(input) * 0.7,
                            this.currentRecipe.value().optimalSpeed(input) * 0.7
                    ) : 1;
        }
        return 0;
    }

    @Override
    public @Nullable Component getCurrentState() {
        return this.state;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.setTitle(GuiTextures.GRINDER.apply(GrinderBlockEntity.this.getBlockState().getBlock().getName()));
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.GRINDING));

            this.setSlotRedirect(4, new Slot(GrinderBlockEntity.this, 0, 0, 0));
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            this.setSlotRedirect(21, new FurnaceResultSlot(player, GrinderBlockEntity.this, 1, 1, 0));
            this.setSlotRedirect(22, new FurnaceResultSlot(player, GrinderBlockEntity.this, 2, 2, 0));
            this.setSlotRedirect(23, new FurnaceResultSlot(player, GrinderBlockEntity.this, 3, 3, 0));
            this.open();
        }

        private float progress() {
            return GrinderBlockEntity.this.currentRecipe != null
                    ? (float) Mth.clamp(GrinderBlockEntity.this.process / GrinderBlockEntity.this.currentRecipe.value().grindTime(new GrindingInput(GrinderBlockEntity.this.stacks.getFirst())), 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(GrinderBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            super.onTick();
        }
    }
}
