package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;

public abstract class AbstracterCableBlock extends CableNetworkBlock implements EntityBlock, CableConnectable {
    public static final int DEFAULT_COLOR = 0xbbbbbb;

    public AbstracterCableBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ConventionalItemTags.DYES) && setColor(state, world, pos, FactoryItems.CABLE.downSampleColor(DyeColorExtra.getColor(stack)))) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            world.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS);
            return InteractionResult.SUCCESS_SERVER;
        } else if (stack.is(FactoryItems.TREATED_DRIED_KELP) && setColor(state, world, pos, DEFAULT_COLOR)) {
            world.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        var stack = super.getCloneItemStack(world, pos, state, includeData);
        if (world.getBlockEntity(pos) instanceof ColorProvider be && !be.isDefaultColor()) {
            ColoredItem.setColor(stack, be.getColor());
        }
        return stack;
    }

    public boolean setColor(BlockState state, Level world, BlockPos pos, int color) {
        color = FactoryItems.CABLE.downSampleColor(color);
        if (world.getBlockEntity(pos) instanceof ColorProvider provider && provider.getColor() != color) {
            provider.setColor(color);
            return true;
        }

        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            if (itemStack.getItem() instanceof ColoredItem) {
                be.setColor(FactoryItems.CABLE.getItemColor(itemStack));
            }
        }
        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    public static int getColor(LevelReader world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof ColorProvider be ? be.getColor() : DEFAULT_COLOR;
    }

    protected boolean canConnectTo(LevelReader world, int ownColor, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        return neighborState.getBlock() instanceof CableConnectable connectable && connectable.canCableConnect(world, ownColor, neighborPos, neighborState, direction);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    public abstract EnumSet<Direction> getDirections(BlockState state);

    protected abstract boolean isDirectionBlocked(BlockState state, Direction direction);

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            return be.getColor() == cableColor || be.isDefaultColor() || cableColor == DEFAULT_COLOR;
        }
        return true;
    }

    public boolean hasCable(BlockState state) {
        return true;
    }

    protected abstract boolean checkModelDirection(BlockState state, Direction direction);


    public static class BaseCableModel extends BlockModel {
        private final ItemDisplayElement cable;
        private int color = AbstracterCableBlock.DEFAULT_COLOR;
        private BlockState state;

        public BaseCableModel(BlockState state) {
            this.cable = ItemDisplayElementUtil.createSimple();
            this.cable.setViewRange(0.5f);
            this.state = state;
            updateModel();
            if (((AbstracterCableBlock) state.getBlock()).hasCable(state)) {
                this.addElement(this.cable);
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                this.setState(this.blockState());
            }
        }

        protected void setState(BlockState blockState) {
            this.state = blockState;
            if (this.hasCable(state)) {
                this.addElement(this.cable);
            } else {
                this.removeElement(this.cable);
            }
            updateModel();
        }


        protected final boolean hasCable(BlockState state) {
            return ((AbstracterCableBlock) state.getBlock()).hasCable(state);
        }

        protected void updateModel() {
            var stack = getModel(this.state, ((AbstracterCableBlock) state.getBlock())::checkModelDirection);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of(this.color)));
            this.cable.setItem(stack);

            if (this.cable.getHolder() == this && this.color >= 0) {
                this.cable.tick();
            }
        }

        public void setColor(int color) {
            this.color = color;
            updateModel();
        }

        public ItemStack getModel(BlockState state, BiPredicate<BlockState, Direction> directionPredicate) {
            return FactoryModels.COLORED_CABLE.get(state, directionPredicate).copy();
        }
    }
}
