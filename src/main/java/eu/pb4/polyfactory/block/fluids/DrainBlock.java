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
import eu.pb4.polyfactory.mixin.ExperienceOrbAccessor;
import eu.pb4.polyfactory.models.fluid.TopFluidViewModel;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class DrainBlock extends Block implements FactoryBlock, PipeConnectable, EntityBlock {
    public DrainBlock(Properties settings) {
        super(settings);
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return dir != Direction.UP;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof FilledStateProvider be) {
            return (int) ((be.getFilledAmount() * 15) / be.getFillCapacity());
        }
        return 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (world instanceof ServerLevel serverWorld && world.getBlockEntity(pos) instanceof DrainBlockEntity be) {
            if ((stack.isEmpty() || (ItemStack.isSameItemSameComponents(stack, be.catalyst()) && stack.getCount() < stack.getMaxStackSize()))
                    && hit.getDirection() == Direction.UP && !be.catalyst().isEmpty()) {
                if (stack.isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, be.catalyst());
                } else {
                    stack.grow(1);
                }
                be.setCatalyst(ItemStack.EMPTY);
                return InteractionResult.SUCCESS_SERVER;
            } else if (stack.is(FactoryItemTags.DRAIN_CATALYST) && hit.getDirection() == Direction.UP && be.catalyst().isEmpty()) {
                be.setCatalyst(stack.copyWithCount(1));
                stack.consume(1, player);
                return InteractionResult.SUCCESS_SERVER;
            }

            var container = be.getFluidContainer();
            var copy = stack.copy();
            var input = DrainInput.of(copy, be.catalyst(), container, !(player instanceof FakePlayer));
            var optional = serverWorld.recipeAccess().getRecipeFor(FactoryRecipeTypes.DRAIN, input, world);
            if (optional.isEmpty()) {
                return super.useWithoutItem(state, world, pos, player, hit);
            }
            var recipe = optional.get().value();
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, optional.get().id(), List.of(stack.copy(), be.catalyst()));
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.DRAIN_USE);
            }
            var itemOut = recipe.assemble(input, player.registryAccess());
            for (var fluid : recipe.fluidInput(input)) {
                container.extract(fluid, false);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, FactoryUtil.exchangeStack(stack, recipe.decreasedInputItemAmount(input), player, itemOut));
            for (var fluid : recipe.fluidOutput(input)) {
                container.insert(fluid, false);
            }
            world.playSound(null, pos, recipe.soundEvent().value(), SoundSource.BLOCKS);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }


    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (world.getBlockEntity(pos) instanceof DrainBlockEntity be) {
            if (entity instanceof ExperienceOrb xp && be.catalyst().is(FactoryItemTags.XP_CONVERSION_CATALYST)) {
                var amount = xp.getValue();

                if (be.getFluidContainer().canInsert(FactoryFluids.EXPERIENCE.defaultInstance(), FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, true)) {
                    be.getFluidContainer().insert(FactoryFluids.EXPERIENCE.defaultInstance(), FluidBehaviours.EXPERIENCE_ORB_TO_FLUID, false);
                    if (amount - 1 <= 0) {
                        xp.discard();
                    } else {
                        ((ExperienceOrbAccessor) xp).callSetValue(amount - 1);
                    }
                }
            }
        }

        super.stepOn(world, pos, state, entity);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return DrainBlockEntity::ticker;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DrainBlockEntity(pos, state);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_BLOCK.defaultBlockState();
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
