package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class DrainBlock extends Block implements FactoryBlock, PipeConnectable, BlockEntityProvider {
    public DrainBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPipeConnect(WorldAccess world, BlockPos pos, BlockState state, Direction dir) {
        return dir != Direction.UP;
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof DrainBlockEntity be) {
            var copy = stack.copy();
            var x = be.getFluidContainer().interactWith((ServerPlayerEntity) player, player.getMainHandStack());
            if (x == null) {
                return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (stack.isEmpty() && ItemStack.areEqual(stack, copy)) {
                return ItemActionResult.FAIL;
            }

            if (stack.isEmpty()) {
                player.setStackInHand(Hand.MAIN_HAND, x);
            } else if (!x.isEmpty()) {
                if (player.isCreative()) {
                    if (!player.getInventory().contains(x)) {
                        player.getInventory().insertStack(x);
                    }
                } else {
                    player.getInventory().offerOrDrop(x);
                }
            }
            return ItemActionResult.SUCCESS;
        }

        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
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
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.COPPER_BLOCK.getDefaultState();
    }

    public static final class Model extends BlockModel {
        private final ItemDisplayElement main;
        private final ItemDisplayElement fluid;
        private FluidInstance<?> currentFluid = null;
        private float positionFluid = -1;
        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.main.setScale(new Vector3f(2f));
            this.fluid = ItemDisplayElementUtil.createSimple();

            this.addElement(this.main);
            this.addElement(this.fluid);
        }

        public void setFluid(@Nullable FluidInstance type, float position) {
            if (type == null || position < 0.01) {
                this.fluid.setItem(ItemStack.EMPTY);
                this.currentFluid = null;
                this.positionFluid = -1;
            }
            if (this.currentFluid != type) {
                this.fluid.setItem(FactoryModels.FLAT_FULL.get(type));
                this.currentFluid = type;
            }
            if (this.positionFluid != position) {
                this.fluid.setTranslation(new Vector3f(0, -6f / 16f + position * 10f / 16f, 0));
                this.positionFluid = position;
            }
        }
    }
}
