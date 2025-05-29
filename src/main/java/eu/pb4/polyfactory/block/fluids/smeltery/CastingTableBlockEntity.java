package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.casting.CastingRecipe;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CastingTableBlockEntity extends LockableBlockEntity implements MinimalSidedInventory {

    public static final int OUTPUT_FIRST = 1;
    public static final int INPUT_FIRST = 0;
    private static final int[] OUTPUT_SLOTS = {1};
    private static final int[] INPUT_SLOTS = {0};
    protected final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(2, ItemStack.EMPTY);
    protected double process = 0;
    @Nullable
    protected RecipeEntry<CastingRecipe> currentRecipe = null;
    private boolean active;
    private CastingTableBlock.Model model;
    private boolean activate = false;
    private boolean isCooling;
    private SmelteryFaucedBlock.FaucedProvider output = SmelteryFaucedBlock.FaucedProvider.EMPTY;

    public CastingTableBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CASTING_TABLE, pos, state);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (CastingTableBlockEntity) t;

        if (self.model == null) {
            self.model = (CastingTableBlock.Model) BlockBoundAttachment.get(world, pos).holder();
        }

        self.model.setMold(self.getStack(0));
        self.model.setOutput(self.getStack(1));

        if (!self.output.isValid()) {
            self.output.setActiveFluid(null);
            self.output = SmelteryFaucedBlock.FaucedProvider.EMPTY;
            if (self.activate) {
                if (world.getBlockState(pos.up()).isOf(FactoryBlocks.SMELTERY_FAUCED)) {
                    self.output = SmelteryFaucedBlock.getOutput(world.getBlockState(pos.up()), (ServerWorld) world, pos.up());
                }

                if (!self.output.isValid()) {
                    self.activate = false;
                }
            }
        }

        if (self.output == SmelteryFaucedBlock.FaucedProvider.EMPTY || self.isInputEmpty() || !self.isOutputEmpty() || !self.activate) {
            self.process = 0;
            self.model.setProgress(false, 0, null);
            self.activate = false;
            self.active = false;
            self.isCooling = false;
            self.model.tick();
            self.output.setActiveFluid(null);
            self.output = SmelteryFaucedBlock.FaucedProvider.EMPTY;
            return;
        }

        var inputStack = self.getStack(INPUT_FIRST);

        var input = self.asInput();

        if (self.currentRecipe == null || !self.currentRecipe.value().matches(input, world)) {
            self.process = 0;
            self.currentRecipe = ((ServerWorld) world).getRecipeManager().getFirstMatch(FactoryRecipeTypes.CASTING, input, world).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.model.tick();
                self.isCooling = false;
                self.output.setActiveFluid(null);
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
            self.output.setActiveFluid(null);

            if (coolingTime > 0 && !self.isCooling) {
                self.isCooling = true;
                self.process = 0;
                self.markDirty();
                return;
            }

            var itemOut = self.currentRecipe.value().craft(input, world.getRegistryManager());
            self.setStack(OUTPUT_FIRST, itemOut);

            if (FactoryUtil.getClosestPlayer(world, pos, 16) instanceof ServerPlayerEntity serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CASTING_METAL);
                Criteria.RECIPE_CRAFTED.trigger(serverPlayer, self.currentRecipe.id(), List.of(inputStack.copy()));
            }
            inputStack.decrement(self.currentRecipe.value().decreasedInputItemAmount(input));
            var damage = self.currentRecipe.value().damageInputItemAmount(input);

            if (damage > 0) {
                var x = inputStack.copy();
                inputStack.damage(damage, (ServerWorld) world, null, Consumers.nop());
                if (inputStack.isEmpty()) {
                    if (x.contains(DataComponentTypes.BREAK_SOUND)) {
                        world.playSound(null, pos, x.get(DataComponentTypes.BREAK_SOUND).value(), SoundCategory.BLOCKS);
                    }
                    ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, x),
                            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.51, 5,
                            0.2, 0, 0.2, 0.5);
                }
            }
            if (inputStack.isEmpty()) {
                self.setStack(INPUT_FIRST, ItemStack.EMPTY);
            }

            self.output.extract(self.currentRecipe.value().fluidInput(input));

            world.playSound(null, pos, self.currentRecipe.value().soundEvent().value(), SoundCategory.BLOCKS);
            self.process = 0;
            self.isCooling = false;
            self.model.setProgress(false, 0, null);
            self.markDirty();
        } else {
            self.process += 1;

            markDirty(world, pos, self.getCachedState());
            var fluid = self.currentRecipe.value().fluidInput(input);

            var progress = self.process / time;
            if (!self.isCooling) {
                ((ServerWorld) world).spawnParticles(fluid.instance().particle(),
                        pos.getX() + 0.5, pos.getY() + 1 + 4 / 16f, pos.getZ() + 0.5, 0,
                        0, -1, 0, 0.1);
                self.output.setActiveFluid(fluid.instance());
            }

            self.model.setProgress(self.isCooling, progress, fluid.instance());
        }
    }

    private SingleItemWithFluid asInput() {
        return new SingleItemWithFluid(this.getStack(INPUT_FIRST).copy(), this.output.getFluidContainerInput(), (ServerWorld) world);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Inventories.writeNbt(nbt, this.stacks, lookup);
        if (this.activate) {
            nbt.putDouble("Progress", this.process);
            nbt.putBoolean("is_cooling", this.isCooling);
        }

        super.writeNbt(nbt, lookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        Inventories.readNbt(nbt, this.stacks, lookup);
        if (nbt.contains("Progress")) {
            this.process = nbt.getDouble("Progress", 0);
            this.isCooling = nbt.getBoolean("is_cooling", false);
            this.activate = true;
        }
        super.readNbt(nbt, lookup);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[0];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    public boolean isInputEmpty() {
        return this.stacks.get(0).isEmpty();
    }

    public boolean isOutputEmpty() {
        return this.stacks.get(1).isEmpty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public int getMaxCount(ItemStack stack) {
        return 1;
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.stacks;
    }

    public ActionResult activate(SmelteryFaucedBlock.FaucedProvider provider) {
        if (this.activate || this.isInputEmpty() || !this.isOutputEmpty() || !provider.isValid()) {
            return ActionResult.FAIL;
        }
        this.output = provider;
        this.activate = true;
        return ActionResult.SUCCESS_SERVER;
    }
}
