package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class SmelteryCoreBlock extends Block implements FactoryBlock {
    public static EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    private static final Set<Vec3i> SUGGESTED_STEEL = Set.of(
        new Vec3i(-1, 0, 1),
        new Vec3i(1, 0, -1),
        new Vec3i(0, 1, 0)
    );

    public SmelteryCoreBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected ActionResult onUse(BlockState state, World worldx, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (state.get(FACING) != hit.getSide()) {
            return ActionResult.PASS;
        }

        var world = (ServerWorld) worldx;
        var center = pos.offset(state.get(FACING).getOpposite());
        if (FactoryBlocks.SMELTERY.placeSmeltery(world, center)) {
            world.playSound(null, center, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS);
            return ActionResult.SUCCESS_SERVER;
        }

        world.playSound(null, center, SoundEvents.ITEM_SHIELD_BLOCK.value(), SoundCategory.BLOCKS);
        var currentBlocks = new HashMap<Vec3i, BlockState>();

        int bricks = 0;
        int steel = 0;

        int i = 0;
        for (var blockPos : BlockPos.iterate(center.add(-1, -1, -1), center.add(1, 1, 1))) {
            if (pos.equals(blockPos)) {
                continue;
            }

            var current = world.getBlockState(blockPos);
            if (current.isOf(Blocks.DEEPSLATE_BRICKS)) bricks++;
            else if (current.isOf(FactoryBlocks.STEEL_BLOCK)) steel++;
            currentBlocks.put(blockPos.subtract(center), current);
        }
        var highlight = new HashMap<Vec3i, Pair<BlockState, Boolean>>();

        //if (!currentBlocks.get(Vec3i.ZERO).isOf(Blocks.DEEPSLATE_BRICKS) && !currentBlocks.get(Vec3i.ZERO).isOf(FactoryBlocks.STEEL_BLOCK)) {
        //    highlight.put(Vec3i.ZERO, new Pair<>(Blocks.AIR.getDefaultState(), !currentBlocks.get(Vec3i.ZERO).isAir()));
        //}

        if (steel < SmelteryBlock.STEEL_BLOCKS) {
            for (var sug : SUGGESTED_STEEL) {
                var current = currentBlocks.get(sug);
                if (!current.isOf(FactoryBlocks.STEEL_BLOCK) && (bricks > SmelteryBlock.DEEPSLATE_BRICK_BLOCKS || !current.isOf(Blocks.DEEPSLATE_BRICKS))) {
                    if (current.isOf(Blocks.DEEPSLATE_BRICKS)) {
                        bricks--;
                    }
                    highlight.put(sug, new Pair<>(FactoryBlocks.STEEL_BLOCK.getDefaultState(), !current.isAir()));
                    steel++;
                    if (steel >= SmelteryBlock.STEEL_BLOCKS) {
                        break;
                    }
                }
            }

            if (steel < SmelteryBlock.STEEL_BLOCKS) {
                for (var key : currentBlocks.keySet()) {
                    if (highlight.containsKey(key)) {
                        continue;
                    }

                    var current = currentBlocks.get(key);
                    if (!current.isOf(FactoryBlocks.STEEL_BLOCK) && (bricks > SmelteryBlock.DEEPSLATE_BRICK_BLOCKS || !current.isOf(Blocks.DEEPSLATE_BRICKS))) {
                        if (current.isOf(Blocks.DEEPSLATE_BRICKS)) {
                            bricks--;
                        }
                        highlight.put(key, new Pair<>(FactoryBlocks.STEEL_BLOCK.getDefaultState(), !current.isAir()));
                        steel++;
                        if (steel >= SmelteryBlock.STEEL_BLOCKS) {
                            break;
                        }
                    }
                }
            }
        }

        if (bricks < SmelteryBlock.DEEPSLATE_BRICK_BLOCKS) {
            for (var key : List.copyOf(currentBlocks.keySet())) {
                if (SUGGESTED_STEEL.contains(key) || key.equals(Vec3i.ZERO) || highlight.containsKey(key)) {
                    continue;
                }
                var current = currentBlocks.get(key);
                if ((steel > SmelteryBlock.STEEL_BLOCKS || !current.isOf(FactoryBlocks.STEEL_BLOCK)) && (!current.isOf(Blocks.DEEPSLATE_BRICKS))) {
                    highlight.put(key, new Pair<>(Blocks.DEEPSLATE_BRICKS.getDefaultState(), !current.isAir()));
                    bricks++;
                    if (current.isOf(FactoryBlocks.STEEL_BLOCK)) {
                        steel--;
                    }
                    if (bricks >= SmelteryBlock.DEEPSLATE_BRICK_BLOCKS) {
                        break;
                    }
                }
            }
            if (bricks < SmelteryBlock.DEEPSLATE_BRICK_BLOCKS) {
                for (var key : SUGGESTED_STEEL) {
                    var current = currentBlocks.get(key);
                    if (!current.isOf(Blocks.DEEPSLATE_BRICKS)) {
                        highlight.put(key, new Pair<>(Blocks.DEEPSLATE_BRICKS.getDefaultState(), !current.isAir()));
                        bricks++;
                        if (bricks >= SmelteryBlock.DEEPSLATE_BRICK_BLOCKS) {
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

            if (!x.isOf(Blocks.DEEPSLATE_BRICKS) && !x.isOf(FactoryBlocks.STEEL_BLOCK) && !highlight.containsKey(key)) {
                highlight.put(key, new Pair<>(Blocks.DEEPSLATE_BRICKS.getDefaultState(), !x.isAir()));
            }
        }

        var model = BlockAwareAttachment.get(world, pos);

        if (model != null && model.holder() instanceof Model model1) {
            model1.setHighlight(highlight);
        }

        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.DEEPSLATE_BRICKS.getDefaultState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
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
            var dir = state.get(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.getPositiveHorizontalDegrees();
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

        public void setHighlight(HashMap<Vec3i, Pair<BlockState, Boolean>> highlight) {
            if (!this.highlights.isEmpty()) {
                return;
            }

            this.highlightTimer = 60;

            highlight.forEach((key, pair) -> {
                if (pair.getLeft().isAir() && !pair.getRight()) {
                    return;
                }
                var element = ItemDisplayElementUtil.createSimple();
                if (pair.getLeft().isAir()) {
                    element.setItem(Items.BARRIER.getDefaultStack());
                    element.setScale(new Vector3f(0.5f));
                    element.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
                } else {
                    element.setItem(pair.getLeft().getBlock().asItem().getDefaultStack());
                }
                element.setGlowing(true);
                element.setGlowColorOverride(pair.getRight() ? 0xFF0000 : 0xFFFFFF);
                element.setOffset(Vec3d.of(key).offset(this.blockState().get(FACING).getOpposite(), 1));
                this.highlights.add(element);
                this.addElement(element);
            });
        }
    }
}
