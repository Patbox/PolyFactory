package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.casting.CastingRecipe;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CastingTableBlockEntity extends LockableBlockEntity implements MinimalSidedContainer {

    public static final int OUTPUT_FIRST = 1;
    public static final int INPUT_FIRST = 0;
    private static final int[] OUTPUT_SLOTS = {1};
    private static final int[] INPUT_SLOTS = {0};
    protected final NonNullList<ItemStack> stacks = NonNullList.withSize(2, ItemStack.EMPTY);
    protected double process = 0;
    @Nullable
    protected RecipeHolder<CastingRecipe> currentRecipe = null;
    private boolean active;
    private CastingTableBlock.Model model;
    private boolean activate = false;
    private boolean isCooling;
    private FaucetBlock.FaucedProvider provider = FaucetBlock.FaucedProvider.EMPTY;
    private float rate;

    public CastingTableBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CASTING_TABLE, pos, state);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (CastingTableBlockEntity) t;

        if (self.model == null) {
            self.model = (CastingTableBlock.Model) BlockBoundAttachment.get(world, pos).holder();
        }

        self.model.setMold(self.getItem(0));
        self.model.setOutput(self.getItem(1));

        if (!self.provider.isValid()) {
            self.provider.setActiveFluid(null);
            self.provider = FaucetBlock.FaucedProvider.EMPTY;
            if (self.activate) {
                if (world.getBlockState(pos.above()).is(FactoryBlocks.FAUCET)) {
                    self.provider = FaucetBlock.getOutput(world.getBlockState(pos.above()), (ServerLevel) world, pos.above());
                }

                if (!self.provider.isValid()) {
                    self.activate = false;
                }
            }
        }

        if (self.provider == FaucetBlock.FaucedProvider.EMPTY || !self.isOutputEmpty() || !self.activate) {
            self.process = 0;
            self.model.setProgress(false, 0, null);
            self.activate = false;
            self.active = false;
            self.isCooling = false;
            self.model.tick();
            self.provider.setActiveFluid(null);
            self.provider = FaucetBlock.FaucedProvider.EMPTY;
            return;
        }

        var inputStack = self.getItem(INPUT_FIRST);

        var input = self.asInput();

        if (self.currentRecipe == null || !self.currentRecipe.value().matches(input, world)) {
            self.process = 0;
            self.currentRecipe = ((ServerLevel) world).recipeAccess().getRecipeFor(FactoryRecipeTypes.CASTING, input, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.model.tick();
                self.isCooling = false;
                self.provider.setActiveFluid(null);
                self.model.setProgress(false, 0, null);
                self.activate = false;
                return;
            }
        }
        self.active = true;
        self.model.tick();

        var coolingTime = self.currentRecipe.value().coolingTime(input);

        var time = self.isCooling ? coolingTime : self.currentRecipe.value().time(input);

        if (self.process >= time) {
            self.provider.setActiveFluid(null);

            if (coolingTime > 0 && !self.isCooling) {
                self.isCooling = true;
                self.process = 0;
                self.rate = 1;
                self.setChanged();
                return;
            }

            var itemOut = self.currentRecipe.value().assemble(input, world.registryAccess());
            self.setItem(OUTPUT_FIRST, itemOut);

            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayer serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CASTING_METAL);
                if (itemOut.is(FactoryItemTags.MOLDS)) {
                    TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CASTING_MOLD);
                }
                CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, self.currentRecipe.id(), List.of(inputStack.copy()));
            }
            inputStack.shrink(self.currentRecipe.value().decreasedInputItemAmount(input));
            var damage = self.currentRecipe.value().damageInputItemAmount(input);

            if (damage > 0) {
                var x = inputStack.copy();
                inputStack.hurtAndBreak(damage, (ServerLevel) world, null, Consumers.nop());
                if (inputStack.isEmpty()) {
                    if (x.has(DataComponents.BREAK_SOUND)) {
                        world.playSound(null, pos, x.get(DataComponents.BREAK_SOUND).value(), SoundSource.BLOCKS);
                    }
                    ((ServerLevel) world).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, x),
                            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.51, 5,
                            0.2, 0, 0.2, 0.5);
                }
            }
            if (inputStack.isEmpty()) {
                self.setItem(INPUT_FIRST, ItemStack.EMPTY);
            }

            self.provider.extract(self.currentRecipe.value().fluidInput(input));

            world.playSound(null, pos, self.currentRecipe.value().soundEvent().value(), SoundSource.BLOCKS);
            self.process = 0;
            self.isCooling = false;
            self.model.setProgress(false, 0, null);
            self.model.setMold(self.getItem(0));
            self.model.setOutput(self.getItem(1));
            self.setChanged();
        } else {
            self.process += self.rate;

            setChanged(world, pos, self.getBlockState());
            var fluid = self.currentRecipe.value().fluidInput(input);

            var progress = self.process / time;
            if (!self.isCooling) {
                ((ServerLevel) world).sendParticles(fluid.instance().particle(),
                        pos.getX() + 0.5 + self.provider.direction().getStepX() / 16f,
                        pos.getY() + 1 + 4 / 16f,
                        pos.getZ() + 0.5 + self.provider.direction().getStepZ() / 16f,
                        0,
                        0, -1, 0, 0.1);
                self.provider.setActiveFluid(fluid.instance());
            }

            self.model.setProgress(self.isCooling, progress, fluid.instance());
        }
    }

    private SingleItemWithFluid asInput() {
        return new SingleItemWithFluid(this.getItem(INPUT_FIRST).copy(), this.provider.getFluidContainerInput(), (ServerLevel) level);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.stacks);
        if (this.activate) {
            view.putDouble("Progress", this.process);
            view.putBoolean("is_cooling", this.isCooling);
        }
        view.putFloat("rate", this.rate);

        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        ContainerHelper.loadAllItems(view, this.stacks);
        if (view.getDoubleOr("Progress", Double.POSITIVE_INFINITY) != Double.POSITIVE_INFINITY) {
            this.process = view.getDoubleOr("Progress", 0);
            this.isCooling = view.getBooleanOr("is_cooling", false);
            this.activate = true;
        }
        this.rate = view.getFloatOr("rate", 1f);

        super.loadAdditional(view);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.provider != null) {
            this.provider.setActiveFluid(null);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_FIRST;
    }

    public boolean isInputEmpty() {
        return this.stacks.get(0).isEmpty();
    }

    public boolean isOutputEmpty() {
        return this.stacks.get(1).isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    public InteractionResult activate(FaucetBlock.FaucedProvider provider, float rate) {
        if (this.isInputEmpty() && !this.isOutputEmpty()) {
            var input = new SingleItemWithFluid(this.getItem(1), provider.getFluidContainerInput(), (ServerLevel) this.level);

            if ((this.currentRecipe != null && this.currentRecipe.value().matches(input, input.world()))
                    || (this.currentRecipe = ((ServerLevel) level).recipeAccess().getRecipeFor(FactoryRecipeTypes.CASTING, input, level).orElse(null)) != null) {
                this.setItem(0, this.getItem(1));
                this.setItem(1, ItemStack.EMPTY);
            }
        }

        if (this.activate || !this.isOutputEmpty() || !provider.isValid()) {
            return InteractionResult.FAIL;
        }
        this.provider = provider;
        this.activate = true;
        this.rate = rate;
        return InteractionResult.SUCCESS_SERVER;
    }
}
