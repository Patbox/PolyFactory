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
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CastingCauldronBlockEntity extends LockableBlockEntity implements SingleStackInventory {

    protected ItemStack stack = ItemStack.EMPTY;
    protected double process = 0;
    @Nullable
    protected RecipeEntry<CauldronCastingRecipe> currentRecipe = null;
    private CastingCauldronBlock.Model model;
    private boolean isCooling;
    private FaucedBlock.FaucedProvider provider = FaucedBlock.FaucedProvider.EMPTY;
    private boolean findRecipe = false;

    public CastingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CASTING_CAULDRON, pos, state);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (CastingCauldronBlockEntity) t;

        if (self.model == null) {
            self.model = (CastingCauldronBlock.Model) BlockBoundAttachment.get(world, pos).holder();
        }

        self.model.setOutput(self.getStack());
        var input = self.asInput();

        if (self.provider == FaucedBlock.FaucedProvider.EMPTY) {
            if (world.getBlockState(pos.up()).isOf(FactoryBlocks.FAUCED)) {
                self.provider = FaucedBlock.getOutput(world.getBlockState(pos.up()), (ServerWorld) world, pos.up());
            } else {
                if (self.stack.isEmpty()) {
                    world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                }
                return;
            }
        }

        if (self.findRecipe) {
            self.currentRecipe = ((ServerWorld) world).getRecipeManager().getFirstMatch(FactoryRecipeTypes.CASTING_CAULDRON, input, world).orElse(null);
        }

        if (self.currentRecipe == null && self.stack.isEmpty()) {
            world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
            return;
        } else if (self.currentRecipe == null || !self.provider.isValid() || !self.currentRecipe.value().matches(input, world)) {
            self.currentRecipe = null;
            self.provider.setActiveFluid(null);
            self.provider = FaucedBlock.FaucedProvider.EMPTY;
            if (self.stack.isEmpty()) {
                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
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
                self.markDirty();
                return;
            }

            var output = self.currentRecipe.value().craft(input, world.getRegistryManager());
            self.setStack(output);

            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayerEntity serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CASTING_METAL);
                Criteria.RECIPE_CRAFTED.trigger(serverPlayer, self.currentRecipe.id(), List.of());
            }

            self.provider.extract(self.currentRecipe.value().fluidInput(input));

            world.playSound(null, pos, self.currentRecipe.value().soundEvent().value(), SoundCategory.BLOCKS);
            self.process = 0;
            self.isCooling = false;
            self.model.setProgress(false, 0, null);
            self.model.setOutput(self.getStack());
            self.findRecipe = false;
            self.currentRecipe = null;
            self.markDirty();
        } else {
            self.process += 1;

            markDirty(world, pos, self.getCachedState());
            var fluid = self.currentRecipe.value().fluidInput(input);

            var progress = self.process / time;
            if (!self.isCooling) {
                ((ServerWorld) world).spawnParticles(fluid.instance().particle(),
                        pos.getX() + 0.5 + self.provider.direction().getOffsetX() / 16f,
                        pos.getY() + 1 + 4 / 16f,
                        pos.getZ() + 0.5 + self.provider.direction().getOffsetZ() / 16f,
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
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);
        if (this.provider != null) {
            this.provider.setActiveFluid(null);
        }
    }

    @Override
    protected void writeData(WriteView view) {
        view.put("stack", ItemStack.OPTIONAL_CODEC, this.stack);
        if (this.currentRecipe != null || this.findRecipe) {
            view.putDouble("Progress", this.process);
            view.putBoolean("is_cooling", this.isCooling);
        }

        super.writeData(view);
    }

    @Override
    public void readData(ReadView view) {
        this.stack = view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        if (view.getDouble("Progress", Double.POSITIVE_INFINITY) != Double.POSITIVE_INFINITY) {
            this.process = view.getDouble("Progress", 0);
            this.isCooling = view.getBoolean("is_cooling", false);
            this.findRecipe = true;
        }
        super.readData(view);
    }

    @Override
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
        this.markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[] { 0 };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }


    public boolean isOutputEmpty() {
        return this.stack.isEmpty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public int getMaxCount(ItemStack stack) {
        return 1;
    }

    public void setup(RecipeEntry<CauldronCastingRecipe> recipe, FaucedBlock.FaucedProvider provider) {
        this.currentRecipe = recipe;
        this.provider = provider;
    }
}
