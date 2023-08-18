package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.FactoryData;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class BlockDataProviderBlock extends DataNetworkBlock implements PolymerBlock, VirtualDestroyStage.Marker, BlockEntityProvider, BlockWithElementHolder, DataProvider {
    public static DirectionProperty FACING = Properties.FACING;
    public BlockDataProviderBlock(Settings settings) {
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
        return super.getPlacementState(ctx).with(FACING, ctx.getSide().getOpposite());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelProviderDirectionNode(state.get(FACING).getOpposite(), 0));
    }

    @Override
    public @Nullable FactoryData provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel) {
        if (world.getBlockEntity(selfPos) instanceof ProviderDataCacheBlockEntity be && be.channel() == channel) {
            return be.lastData;
        }
        return null;
    }

    public void sendData(ServerWorld world, BlockPos selfPos, FactoryData data) {
        if (world.getBlockEntity(selfPos) instanceof ProviderDataCacheBlockEntity be) {
            be.lastData = data;
            NetworkComponent.Data.getLogic(world, selfPos).pushDataUpdate(be.channel(), data);

        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ProviderDataCacheBlockEntity(pos, state);
    }

    private final class Model extends BaseModel {
        private final LodItemDisplayElement base;

        private Model(BlockState state) {
            this.base = LodItemDisplayElement.createSimple(FactoryItems.BLOCK_DATA_PROVIDER);
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
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(BlockBoundAttachment.get(this).getBlockState());
            }
        }
    }
}
