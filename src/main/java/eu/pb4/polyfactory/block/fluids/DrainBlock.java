package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.mixin.ExperienceOrbEntityAccessor;
import eu.pb4.polyfactory.models.fluid.TopFluidViewModel;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class DrainBlock extends Block implements FactoryBlock, PipeConnectable, BlockEntityProvider {
    public DrainBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPipeConnect(WorldView world, BlockPos pos, BlockState state, Direction dir) {
        return dir != Direction.UP;
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof FilledStateProvider be) {
            return (int) ((be.getFilledAmount() * 15) / be.getFillCapacity());
        }
        return 0;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var stack = player.getStackInHand(Hand.MAIN_HAND);
        if (world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof DrainBlockEntity be) {
            if ((stack.isEmpty() || (ItemStack.areItemsAndComponentsEqual(stack, be.catalyst()) && stack.getCount() < stack.getMaxCount()))
                    && hit.getSide() == Direction.UP && !be.catalyst().isEmpty()) {
                if (stack.isEmpty()) {
                    player.setStackInHand(Hand.MAIN_HAND, be.catalyst());
                } else {
                    stack.increment(1);
                }
                be.setCatalyst(ItemStack.EMPTY);
                return ActionResult.SUCCESS_SERVER;
            } else if (stack.isIn(FactoryItemTags.DRAIN_CATALYST) && hit.getSide() == Direction.UP && be.catalyst().isEmpty()) {
                be.setCatalyst(stack.copyWithCount(1));
                stack.decrementUnlessCreative(1, player);
                return ActionResult.SUCCESS_SERVER;
            }

            var container = be.getFluidContainer();
            var copy = stack.copy();
            var input = DrainInput.of(copy, be.catalyst(), container, !(player instanceof FakePlayer));
            var optional = serverWorld.getRecipeManager().getFirstMatch(FactoryRecipeTypes.DRAIN, input, world);
            if (optional.isEmpty()) {
                return super.onUse(state, world, pos, player, hit);
            }
            var recipe = optional.get().value();
            if (player instanceof ServerPlayerEntity serverPlayer) {
                Criteria.RECIPE_CRAFTED.trigger(serverPlayer, optional.get().id(), List.of(stack.copy(), be.catalyst()));
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.DRAIN_USE);
            }
            var itemOut = recipe.craft(input, player.getRegistryManager());
            for (var fluid : recipe.fluidInput(input)) {
                container.extract(fluid, false);
            }
            player.setStackInHand(Hand.MAIN_HAND, FactoryUtil.exchangeStack(stack, recipe.decreasedInputItemAmount(input), player, itemOut));
            for (var fluid : recipe.fluidOutput(input)) {
                container.insert(fluid, false);
            }
            world.playSound(null, pos, recipe.soundEvent().value(), SoundCategory.BLOCKS);
            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUse(state, world, pos, player, hit);
    }


    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (world.getBlockEntity(pos) instanceof DrainBlockEntity be) {
            if (entity instanceof ExperienceOrbEntity xp && be.catalyst().isIn(FactoryItemTags.XP_CONVERSION_CATALYST)) {
                var amount = xp.getValue();

                if (be.getFluidContainer().canInsert(FactoryFluids.EXPERIENCE.defaultInstance(), FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, true)) {
                    be.getFluidContainer().insert(FactoryFluids.EXPERIENCE.defaultInstance(), FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, false);
                    if (amount - 1 <= 0) {
                        xp.discard();
                    } else {
                        ((ExperienceOrbEntityAccessor) xp).callSetValue(amount - 1);
                    }
                }
            }
        }

        super.onSteppedOn(world, pos, state, entity);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return DrainBlockEntity::ticker;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DrainBlockEntity(pos, state);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_BLOCK.getDefaultState();
    }

    public static final class Model extends BlockModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement catalyst;
        private final TopFluidViewModel fluid;
        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2f));
            this.fluid = new TopFluidViewModel(this, -7f / 16f + 0.1f, 11f / 16f, 0.5f);
            this.catalyst = ItemDisplayElementUtil.createSimple();
            this.catalyst.setPitch(-90);
            this.catalyst.setTranslation(new Vector3f(0, 0, 4.5f / 16f));
            this.catalyst.setScale(new Vector3f(12 / 16f));
            this.catalyst.setViewRange(0.4f);
            this.addElement(this.main);
            this.addElement(this.catalyst);
        }

        public void setFluid(@Nullable FluidInstance<?> type, float position) {
            this.fluid.setFluid(type, position);
        }

        public void setCatalyst(ItemStack catalyst) {
            this.catalyst.setItem(catalyst);
        }
    }
}
