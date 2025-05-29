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
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class CastingTableBlock extends Block implements FactoryBlock, BlockEntityProvider {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    public CastingTableBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CastingTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.CASTING_TABLE ? CastingTableBlockEntity::ticker : null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.shouldCancelInteraction() && world.getBlockEntity(pos) instanceof CastingTableBlockEntity be) {
            if (!be.getStack(1).isEmpty()) {
                if (player.getMainHandStack().isEmpty()) {
                    player.equipStack(EquipmentSlot.MAINHAND, be.getStack(1));
                } else {
                    player.giveOrDropStack(be.getStack(1));
                }

                be.setStack(1, ItemStack.EMPTY);
                return ActionResult.SUCCESS_SERVER;
            } else if (!be.getStack(0).isEmpty() && player.getMainHandStack().isEmpty()) {
                player.equipStack(EquipmentSlot.MAINHAND, be.getStack(0));
                be.setStack(0, ItemStack.EMPTY);
                return ActionResult.SUCCESS_SERVER;
            }
        }
        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.shouldCancelInteraction() && world.getBlockEntity(pos) instanceof CastingTableBlockEntity be && !stack.isEmpty() && be.isInputEmpty()) {
            be.setStack(0, stack.copyWithCount(1));
            stack.decrement(1);
            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.ANVIL.getDefaultState();
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement fluid;
        private final ItemDisplayElement mold;
        private final ItemDisplayElement output;
        private double progress;
        private FluidInstance<?> castingFluid;
        private boolean isCooling;

        private Model(ServerWorld world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2));
            this.mold = ItemDisplayElementUtil.createSimple();
            this.mold.setScale(new Vector3f(12 / 16f));
            this.mold.setLeftRotation(new Quaternionf().rotateX(MathHelper.HALF_PI).rotateY(MathHelper.PI));
            this.mold.setTranslation(new Vector3f(0, 7.5f / 16f, 0));
            this.fluid = LodItemDisplayElement.createSimple();
            this.fluid.setViewRange(0.4f);
            this.fluid.setScale(new Vector3f(12 / 16f));
            this.fluid.setOffset(new Vec3d(0, 7.5 / 16f - (2 / 16f / 16f), 0));
            this.output = LodItemDisplayElement.createSimple();
            this.output.setViewRange(0.6f);
            this.output.setLeftRotation(this.mold.getLeftRotation());
            this.output.setScale(new Vector3f(12 / 16f));
            this.output.setOffset(new Vec3d(0, 7.5 / 16f + (1 / 16f / 16f), 0));


            updateStatePos(state);
            this.addElement(this.main);
            this.addElement(this.mold);
            this.addElement(this.fluid);
            this.addElement(this.output);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.get(FACING);
            this.main.setYaw(direction.getPositiveHorizontalDegrees());
            this.fluid.setYaw(direction.getPositiveHorizontalDegrees());
            this.mold.setYaw(direction.getPositiveHorizontalDegrees());
            this.output.setYaw(direction.getPositiveHorizontalDegrees());
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        public void setMold(ItemStack stack) {
            if (ItemStack.areItemsAndComponentsEqual(stack, this.mold.getItem())) return;
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
                var value = MathHelper.clamp((float) (isSolid ? ((process - 0.50f) / 0.50f) : (1 - (process) / 0.50f)), 0, 1);
                var color = isCooling ? ColorHelper.fromFloats(1, 1f, 0.6f + value * 0.4f, 0.5f + value * 0.5f) : 0xFFFFFF;
                this.fluid.setItem(
                        (this.mold.getItem().isIn(FactoryItemTags.CASTING_SMALL_FLUID)
                                ? FactoryModels.FLUID_FLAT_14_SPOUT
                                : FactoryModels.FLUID_FLAT_FULL_SPOUT).get(castingFluid, color, isSolid));
                this.fluid.setTranslation(new Vector3f(0, (float) ((isCooling ? 1 : MathHelper.clamp(process, 0, 1)) - 0.5) / 16.2f * 12 / 16f, 0));
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
            if (ItemStack.areItemsAndComponentsEqual(stack, this.output.getItem())) return;
            this.output.setItem(stack.copyWithCount(1));
            this.output.tick();
        }
    }
}
