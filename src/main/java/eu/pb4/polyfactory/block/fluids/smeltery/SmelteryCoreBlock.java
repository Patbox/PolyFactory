package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.other.BlockWithTooltip;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;


public class SmelteryCoreBlock extends Block implements FactoryBlock, BlockWithTooltip {
    public static EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Set<Vec3i> SUGGESTED_STEEL = Set.of(
        new Vec3i(-1, 0, 1),
        new Vec3i(1, 0, -1),
        new Vec3i(0, 1, 0)
    );

    public SmelteryCoreBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level worldx, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(FACING) != hit.getDirection()) {
            return InteractionResult.PASS;
        }

        var world = (ServerLevel) worldx;
        var center = pos.relative(state.getValue(FACING).getOpposite());
        if (FactoryBlocks.SMELTERY.placeSmeltery(world, center)) {
            world.playSound(null, center, SoundEvents.ANVIL_USE, SoundSource.BLOCKS);
            if (player instanceof ServerPlayer serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.INDUSTRIAL_SMELTERY_CREATED);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        world.playSound(null, center, SoundEvents.SHIELD_BLOCK.value(), SoundSource.BLOCKS);
        var currentBlocks = new HashMap<Vec3i, BlockState>();

        int bricks = 0;
        int steel = 0;

        for (var blockPos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (pos.equals(blockPos)) {
                continue;
            }

            var current = world.getBlockState(blockPos);
            if (current.is(Blocks.DEEPSLATE_BRICKS)) bricks++;
            else if (current.is(FactoryBlocks.STEEL_BLOCK)) steel++;
            currentBlocks.put(blockPos.subtract(center), current);
        }
        var highlight = new HashMap<Vec3i, Tuple<BlockState, Boolean>>();

        if (steel < IndustrialSmelteryBlock.STEEL_BLOCKS) {
            for (var sug : SUGGESTED_STEEL) {
                var current = currentBlocks.get(sug);
                if (!current.is(FactoryBlocks.STEEL_BLOCK) && (bricks > IndustrialSmelteryBlock.DEEPSLATE_BRICK_BLOCKS || !current.is(Blocks.DEEPSLATE_BRICKS))) {
                    if (current.is(Blocks.DEEPSLATE_BRICKS)) {
                        bricks--;
                    }
                    highlight.put(sug, new Tuple<>(FactoryBlocks.STEEL_BLOCK.defaultBlockState(), !current.isAir()));
                    steel++;
                    if (steel >= IndustrialSmelteryBlock.STEEL_BLOCKS) {
                        break;
                    }
                }
            }

            if (steel < IndustrialSmelteryBlock.STEEL_BLOCKS) {
                for (var key : currentBlocks.keySet()) {
                    if (highlight.containsKey(key)) {
                        continue;
                    }

                    var current = currentBlocks.get(key);
                    if (!current.is(FactoryBlocks.STEEL_BLOCK) && (bricks > IndustrialSmelteryBlock.DEEPSLATE_BRICK_BLOCKS || !current.is(Blocks.DEEPSLATE_BRICKS))) {
                        if (current.is(Blocks.DEEPSLATE_BRICKS)) {
                            bricks--;
                        }
                        highlight.put(key, new Tuple<>(FactoryBlocks.STEEL_BLOCK.defaultBlockState(), !current.isAir()));
                        steel++;
                        if (steel >= IndustrialSmelteryBlock.STEEL_BLOCKS) {
                            break;
                        }
                    }
                }
            }
        }

        if (bricks < IndustrialSmelteryBlock.DEEPSLATE_BRICK_BLOCKS) {
            for (var key : List.copyOf(currentBlocks.keySet())) {
                if (SUGGESTED_STEEL.contains(key) || key.equals(Vec3i.ZERO) || highlight.containsKey(key)) {
                    continue;
                }
                var current = currentBlocks.get(key);
                if ((steel > IndustrialSmelteryBlock.STEEL_BLOCKS || !current.is(FactoryBlocks.STEEL_BLOCK)) && (!current.is(Blocks.DEEPSLATE_BRICKS))) {
                    highlight.put(key, new Tuple<>(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), !current.isAir()));
                    bricks++;
                    if (current.is(FactoryBlocks.STEEL_BLOCK)) {
                        steel--;
                    }
                    if (bricks >= IndustrialSmelteryBlock.DEEPSLATE_BRICK_BLOCKS) {
                        break;
                    }
                }
            }
            if (bricks < IndustrialSmelteryBlock.DEEPSLATE_BRICK_BLOCKS) {
                for (var key : SUGGESTED_STEEL) {
                    var current = currentBlocks.get(key);
                    if (!current.is(Blocks.DEEPSLATE_BRICKS)) {
                        highlight.put(key, new Tuple<>(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), !current.isAir()));
                        bricks++;
                        if (bricks >= IndustrialSmelteryBlock.DEEPSLATE_BRICK_BLOCKS) {
                            break;
                        }
                    }
                }
            }
        }

        for (var key : currentBlocks.keySet()) {
            var x = currentBlocks.get(key);

            if (x.isAir() && key.equals(Vec3i.ZERO)) {
                continue;
            }

            if (!x.is(Blocks.DEEPSLATE_BRICKS) && !x.is(FactoryBlocks.STEEL_BLOCK) && !highlight.containsKey(key)) {
                highlight.put(key, new Tuple<>(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), !x.isAir()));
            }
        }

        var model = BlockAwareAttachment.get(world, pos);

        if (model != null && model.holder() instanceof Model model1) {
            model1.setHighlight(highlight);
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.translatable(getDescriptionId() + ".tooltip.1").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(getDescriptionId() + ".tooltip.2", IndustrialSmelteryBlock.DEEPSLATE_BRICK_BLOCKS, IndustrialSmelteryBlock.STEEL_BLOCKS).withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(getDescriptionId() + ".tooltip.3").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable(getDescriptionId() + ".tooltip.4").withStyle(ChatFormatting.GRAY));
    }

    public static final class Model extends BlockModel {
        private final ItemDisplayElement main;
        private final List<ItemDisplayElement> highlights = new ArrayList<>();
        private int highlightTimer = -1;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2f));
            this.updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }

            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                this.tick();
            }
        }

        @Override
        public void tick() {
            if (this.highlightTimer > -1 && this.highlightTimer-- == 0) {
                this.highlights.forEach(this::removeElement);
                this.highlights.clear();
            }

            super.tick();
        }

        public void setHighlight(HashMap<Vec3i, Tuple<BlockState, Boolean>> highlight) {
            if (!this.highlights.isEmpty()) {
                return;
            }

            this.highlightTimer = 60;

            highlight.forEach((key, pair) -> {
                if (pair.getA().isAir() && !pair.getB()) {
                    return;
                }
                var element = ItemDisplayElementUtil.createSimple();
                if (pair.getA().isAir()) {
                    element.setItem(Items.BARRIER.getDefaultInstance());
                    element.setScale(new Vector3f(0.5f));
                    element.setBillboardMode(Display.BillboardConstraints.CENTER);
                } else {
                    element.setItem(pair.getA().getBlock().asItem().getDefaultInstance());
                }
                element.setGlowing(true);
                element.setGlowColorOverride(pair.getB() ? 0xFF0000 : 0xFFFFFF);
                element.setOffset(Vec3.atLowerCornerOf(key).relative(this.blockState().getValue(FACING).getOpposite(), 1));
                this.highlights.add(element);
                this.addElement(element);
            });
        }
    }
}
