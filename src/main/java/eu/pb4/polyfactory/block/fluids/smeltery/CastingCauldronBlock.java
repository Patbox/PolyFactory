package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
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
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class CastingCauldronBlock extends Block implements PolymerBlock, BlockWithElementHolder, BlockEntityProvider {
    public CastingCauldronBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CastingCauldronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.CASTING_CAULDRON ? CastingCauldronBlockEntity::ticker : null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.shouldCancelInteraction() && world.getBlockEntity(pos) instanceof CastingCauldronBlockEntity be) {
            if (!be.getStack().isEmpty()) {
                if (player.getMainHandStack().isEmpty()) {
                    player.equipStack(EquipmentSlot.MAINHAND, be.getStack(1));
                } else {
                    player.giveOrDropStack(be.getStack(1));
                }
                be.setStack(ItemStack.EMPTY);
                world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
                return ActionResult.SUCCESS_SERVER;
            }
        }
        return super.onUse(state, world, pos, player, hit);
    }

    public ActionResult tryCauldronCasting(ServerWorld world, BlockPos pos, FaucedBlock.FaucedProvider provider, float rate) {
        var recipe = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.CASTING_CAULDRON, provider.getFluidContainerInput(), world);
        if (recipe.isEmpty()) {
            return ActionResult.FAIL;
        }

        world.setBlockState(pos, this.getDefaultState());
        if (world.getBlockEntity(pos) instanceof CastingCauldronBlockEntity be) {
            be.setup(recipe.get(), provider, rate);
        }

        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.CAULDRON.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.CAULDRON.getDefaultState();
    }

    @Override
    protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return Items.CAULDRON.getDefaultStack();
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement fluid;
        private final ItemDisplayElement output;
        private double progress;
        private FluidInstance<?> castingFluid;
        private boolean isCooling;

        private Model(ServerWorld world, BlockState state) {
            this.fluid = LodItemDisplayElement.createSimple();
            this.fluid.setViewRange(0.4f);
            this.fluid.setScale(new Vector3f(12 / 16f));
            this.fluid.setOffset(new Vec3d(0, - (1 / 16f / 16f), 0));
            this.output = LodItemDisplayElement.createSimple();
            this.output.setItemDisplayContext(ItemDisplayContext.NONE);
            this.output.setViewRange(0.6f);
            this.output.setScale(new Vector3f(12 / 16f));
            this.output.setOffset(new Vec3d(0, 2 / 16f, 0));


            updateStatePos(state);
            this.addElement(this.fluid);
            this.addElement(this.output);
        }

        private void updateStatePos(BlockState state) {
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        public void setProgress(boolean isCooling, double process, FluidInstance<?> castingFluid) {
            if (this.progress == process && this.castingFluid == castingFluid && this.isCooling == isCooling) {
                return;
            }

            if (castingFluid == null) {
                this.fluid.setItem(ItemStack.EMPTY);
                this.fluid.setTranslation(new Vector3f(0, -0.5f, 0));
            } else {
                var isSolid = process > 0.5 && isCooling;
                var value = MathHelper.clamp((float) (isSolid ? ((process - 0.50f) / 0.50f) : (1 - (process) / 0.50f)), 0, 1);
                var color = isCooling ? ColorHelper.fromFloats(1, 1f, 0.6f + value * 0.4f, 0.5f + value * 0.5f) : 0xFFFFFF;
                this.fluid.setItem(FactoryModels.FLUID_FLAT_FULL_SPOUT.get(castingFluid, color, isSolid));
                this.fluid.setTranslation(new Vector3f(0, (float) ((isCooling ? 1 : MathHelper.clamp(process, 0, 1)) - 0.5) * 12 / 16f + 2 / 16f, 0));
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
