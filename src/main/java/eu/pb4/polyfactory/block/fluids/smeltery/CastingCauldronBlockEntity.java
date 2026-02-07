package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.casting.CauldronCastingRecipe;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SingleStackContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CastingCauldronBlockEntity extends LockableBlockEntity implements SingleStackContainer {

    protected ItemStack stack = ItemStack.EMPTY;
    protected double process = 0;
    @Nullable
    protected RecipeHolder<CauldronCastingRecipe> currentRecipe = null;
    private CastingCauldronBlock.Model model;
    private boolean isCooling;
    private FaucetBlock.FaucedProvider provider = FaucetBlock.FaucedProvider.EMPTY;
    private boolean findRecipe = false;
    private float rate;

    public CastingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CASTING_CAULDRON, pos, state);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (CastingCauldronBlockEntity) t;

        if (self.model == null) {
            self.model = (CastingCauldronBlock.Model) BlockBoundAttachment.get(world, pos).holder();
        }

        self.model.setOutput(self.getStack());
        var input = self.asInput();

        if (self.provider == FaucetBlock.FaucedProvider.EMPTY) {
            if (world.getBlockState(pos.above()).is(FactoryBlocks.FAUCET)) {
                self.provider = FaucetBlock.getOutput(world.getBlockState(pos.above()), (ServerLevel) world, pos.above());
            } else {
                if (self.stack.isEmpty()) {
                    world.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                }
                return;
            }
        }

        if (self.findRecipe) {
            self.currentRecipe = ((ServerLevel) world).recipeAccess().getRecipeFor(FactoryRecipeTypes.CASTING_CAULDRON, input, world).orElse(null);
        }

        if (self.currentRecipe == null && self.stack.isEmpty()) {
            world.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            return;
        } else if (self.currentRecipe == null || !self.provider.isValid() || !self.currentRecipe.value().matches(input, world)) {
            self.currentRecipe = null;
            self.provider.setActiveFluid(null);
            self.provider = FaucetBlock.FaucedProvider.EMPTY;
            if (self.stack.isEmpty()) {
                world.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            }
            return;
        }

        if (!self.stack.isEmpty()) {
            self.currentRecipe = null;
            self.findRecipe = false;
            return;
        }

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

            var output = self.currentRecipe.value().assemble(input, world.registryAccess());
            self.setStack(output);

            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayer serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CASTING_METAL);
                CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, self.currentRecipe.id(), List.of());
            }

            self.provider.extract(self.currentRecipe.value().fluidInput(input));

            world.playSound(null, pos, self.currentRecipe.value().soundEvent().value(), SoundSource.BLOCKS);
            self.process = 0;
            self.isCooling = false;
            self.model.setProgress(false, 0, null);
            self.model.setOutput(self.getStack());
            self.findRecipe = false;
            self.currentRecipe = null;
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

    private FluidContainerInput asInput() {
        return this.provider.getFluidContainerInput();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.provider != null) {
            this.provider.setActiveFluid(null);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        view.store("stack", ItemStack.OPTIONAL_CODEC, this.stack);
        if (this.currentRecipe != null || this.findRecipe) {
            view.putDouble("Progress", this.process);
            view.putBoolean("is_cooling", this.isCooling);
        }
        view.putFloat("rate", this.rate);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.stack = view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        if (view.getDoubleOr("Progress", Double.POSITIVE_INFINITY) != Double.POSITIVE_INFINITY) {
            this.process = view.getDoubleOr("Progress", 0);
            this.isCooling = view.getBooleanOr("is_cooling", false);
            this.findRecipe = true;
        }
        this.rate = view.getFloatOr("rate", 1f);
        super.loadAdditional(view);
    }

    @Override
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
        this.setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] { 0 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }


    public boolean isOutputEmpty() {
        return this.stack.isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    public void setup(RecipeHolder<CauldronCastingRecipe> recipe, FaucetBlock.FaucedProvider provider, float rate) {
        this.currentRecipe = recipe;
        this.provider = provider;
        this.rate = rate;
    }
}
