package eu.pb4.polyfactory.block.electric;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class WitherSkullGeneratorBlock extends NetworkBlock implements FactoryBlock, BlockEntityProvider, CableConnectable, EnergyUser, WrenchableBlock {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final int SCHEDULED_TICK_DELAY = 4;

    public WitherSkullGeneratorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(POWERED, false));
    }

    public static Position getOutputLocation(BlockPointer pointer) {
        Direction direction = pointer.state().get(FACING);
        return pointer.centerPos().add(0.7 * (double) direction.getOffsetX(), 0.7 * (double) direction.getOffsetY(), 0.7 * (double) direction.getOffsetZ());
    }

    @Override
    protected void updateNetworkAt(WorldAccess world, BlockPos pos) {
        Energy.updateEnergyAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof Energy;
    }

    @Override
    public void updateEnergyData(EnergyData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof WitherSkullGeneratorBlockEntity be) {
            be.updateEnergyData(modifier, state, world, pos);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayer() != null && ctx.getPlayer().isSneaking() ? ctx.getSide() : ctx.getSide().getOpposite());
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        boolean bl = world.isReceivingRedstonePower(pos);
        boolean bl2 = state.get(POWERED);
        if (bl && !bl2) {
            world.setBlockState(pos, state.with(POWERED, bl), 2);
        }
    }

    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof WitherSkullGeneratorBlockEntity be) {
            //be.setCustomName(itemStack.getName());
            if (placer instanceof PlayerEntity player) {
                be.owner = player.getGameProfile();
            }
        }
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WitherSkullGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return WitherSkullGeneratorBlockEntity::tick;
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.get(FACING).getOpposite()));
    }

    @Override
    public boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING) == dir.getOpposite();
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(WrenchAction.FACING);
    }

    public static final class Model extends RotationAwareModel {
        private final LodItemDisplayElement base;

        public Model(BlockState state) {
            this.base = LodItemDisplayElement.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.addElement(this.base);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.asRotation();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }


            this.base.setYaw(y);
            this.base.setPitch(p);
            //this.axle.setYaw(y);
            //this.axle.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }
    }
}
