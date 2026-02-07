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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class CastingCauldronBlock extends Block implements PolymerBlock, BlockWithElementHolder, EntityBlock {
    public CastingCauldronBlock(Properties settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CastingCauldronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.CASTING_CAULDRON ? CastingCauldronBlockEntity::ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() && world.getBlockEntity(pos) instanceof CastingCauldronBlockEntity be) {
            if (!be.getStack().isEmpty()) {
                if (player.getMainHandItem().isEmpty()) {
                    player.setItemSlot(EquipmentSlot.MAINHAND, be.getItem(1));
                } else {
                    player.handleExtraItemsCreatedOnUse(be.getItem(1));
                }
                be.setStack(ItemStack.EMPTY);
                world.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        return super.useWithoutItem(state, world, pos, player, hit);
    }

    public InteractionResult tryCauldronCasting(ServerLevel world, BlockPos pos, FaucetBlock.FaucedProvider provider, float rate) {
        var recipe = world.recipeAccess().getRecipeFor(FactoryRecipeTypes.CASTING_CAULDRON, provider.getFluidContainerInput(), world);
        if (recipe.isEmpty()) {
            return InteractionResult.FAIL;
        }

        world.setBlockAndUpdate(pos, this.defaultBlockState());
        if (world.getBlockEntity(pos) instanceof CastingCauldronBlockEntity be) {
            be.setup(recipe.get(), provider, rate);
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.CAULDRON.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.CAULDRON.defaultBlockState();
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return Items.CAULDRON.getDefaultInstance();
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement fluid;
        private final ItemDisplayElement output;
        private double progress;
        private FluidInstance<?> castingFluid;
        private boolean isCooling;

        private Model(ServerLevel world, BlockState state) {
            this.fluid = LodItemDisplayElement.createSimple();
            this.fluid.setViewRange(0.4f);
            this.fluid.setScale(new Vector3f(12 / 16f));
            this.fluid.setOffset(new Vec3(0, - (1 / 16f / 16f), 0));
            this.output = LodItemDisplayElement.createSimple();
            this.output.setItemDisplayContext(ItemDisplayContext.NONE);
            this.output.setViewRange(0.6f);
            this.output.setScale(new Vector3f(12 / 16f));
            this.output.setOffset(new Vec3(0, 2 / 16f, 0));


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
                var value = Mth.clamp((float) (isSolid ? ((process - 0.50f) / 0.50f) : (1 - (process) / 0.50f)), 0, 1);
                var color = isCooling ? ARGB.colorFromFloat(1, 1f, 0.6f + value * 0.4f, 0.5f + value * 0.5f) : 0xFFFFFF;
                this.fluid.setItem(FactoryModels.FLUID_FLAT_FULL_SPOUT.get(castingFluid, color, isSolid));
                this.fluid.setTranslation(new Vector3f(0, (float) ((isCooling ? 1 : Mth.clamp(process, 0, 1)) - 0.5) * 12 / 16f + 2 / 16f, 0));
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
