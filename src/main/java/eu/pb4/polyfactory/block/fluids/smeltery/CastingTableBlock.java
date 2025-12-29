package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class CastingTableBlock extends Block implements FactoryBlock, EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CastingTableBlock(Properties settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CastingTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).setValue(FACING, ctx.getHorizontalDirection());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.CASTING_TABLE ? CastingTableBlockEntity::ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() && world.getBlockEntity(pos) instanceof CastingTableBlockEntity be) {
            if (!be.getItem(1).isEmpty()) {
                if (player.getMainHandItem().isEmpty()) {
                    player.setItemSlot(EquipmentSlot.MAINHAND, be.getItem(1));
                } else {
                    player.handleExtraItemsCreatedOnUse(be.getItem(1));
                }

                be.setItem(1, ItemStack.EMPTY);
                return InteractionResult.SUCCESS_SERVER;
            } else if (!be.getItem(0).isEmpty() && player.getMainHandItem().isEmpty()) {
                player.setItemSlot(EquipmentSlot.MAINHAND, be.getItem(0));
                be.setItem(0, ItemStack.EMPTY);
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() && world.getBlockEntity(pos) instanceof CastingTableBlockEntity be && !stack.isEmpty() && be.isInputEmpty()) {
            be.setItem(0, stack.copyWithCount(1));
            stack.shrink(1);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.CAULDRON.defaultBlockState();
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement fluid;
        private final ItemDisplayElement mold;
        private final ItemDisplayElement output;
        private double progress;
        private FluidInstance<?> castingFluid;
        private boolean isCooling;

        private Model(ServerLevel world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSolid(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2));
            this.mold = ItemDisplayElementUtil.createSimple();
            this.mold.setScale(new Vector3f(12 / 16f));
            this.mold.setLeftRotation(new Quaternionf().rotateX(Mth.HALF_PI).rotateY(Mth.PI));
            this.mold.setTranslation(new Vector3f(0, 7.5f / 16f, 0));
            this.fluid = LodItemDisplayElement.createSimple();
            this.fluid.setViewRange(0.4f);
            this.fluid.setScale(new Vector3f(12 / 16f));
            this.fluid.setOffset(new Vec3(0, 7.5 / 16f - (2 / 16f / 16f), 0));
            this.output = LodItemDisplayElement.createSimple();
            this.output.setViewRange(0.6f);
            this.output.setLeftRotation(this.mold.getLeftRotation());
            this.output.setScale(new Vector3f(12 / 16f));
            this.output.setOffset(new Vec3(0, 7.5 / 16f + (1 / 16f / 16f), 0));


            updateStatePos(state);
            this.addElement(this.main);
            this.addElement(this.mold);
            this.addElement(this.fluid);
            this.addElement(this.output);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(FACING);
            this.main.setYaw(direction.toYRot());
            this.fluid.setYaw(direction.toYRot());
            this.mold.setYaw(direction.toYRot());
            this.output.setYaw(direction.toYRot());
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        public void setMold(ItemStack stack) {
            if (ItemStack.isSameItemSameComponents(stack, this.mold.getItem())) return;
            this.mold.setItem(stack.copyWithCount(1));
            this.mold.tick();
        }

        public void setProgress(boolean isCooling, double process, FluidInstance<?> castingFluid) {
            if (this.progress == process && this.castingFluid == castingFluid && this.isCooling == isCooling) {
                return;
            }

            if (castingFluid == null) {
                this.fluid.setItem(ItemStack.EMPTY);
                this.fluid.setTranslation(new Vector3f(0, -0.1f, 0));
            } else {
                var isSolid = process > 0.5 && isCooling;
                var value = Mth.clamp((float) (isSolid ? ((process - 0.50f) / 0.50f) : (1 - (process) / 0.50f)), 0, 1);
                var color = isCooling ? ARGB.colorFromFloat(1, 1f, 0.6f + value * 0.4f, 0.5f + value * 0.5f) : 0xFFFFFF;
                this.fluid.setItem(
                        (this.mold.getItem().is(FactoryItemTags.CASTING_SMALL_FLUID)
                                ? FactoryModels.FLUID_FLAT_14_SPOUT
                                : FactoryModels.FLUID_FLAT_FULL_SPOUT).get(castingFluid, color, isSolid));
                this.fluid.setTranslation(new Vector3f(0, (float) ((isCooling ? 1 : Mth.clamp(process, 0, 1)) - 0.5) / 16.2f * 12 / 16f, 0));
            }
            if (process > this.progress && !isCooling) {
                this.fluid.startInterpolationIfDirty();
            }

            this.fluid.tick();
            this.progress = process;
            this.castingFluid = castingFluid;
            this.isCooling = isCooling;
        }

        public void setOutput(ItemStack stack) {
            if (ItemStack.isSameItemSameComponents(stack, this.output.getItem())) return;
            this.output.setItem(stack.copyWithCount(1));
            this.output.tick();
        }
    }
}
