package eu.pb4.polyfactory.block.electric;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ElectricMotorBlock extends NetworkBlock implements FactoryBlock, EntityBlock, CableConnectable, RotationUser, EnergyUser, ConfigurableBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public ElectricMotorBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        NetworkComponent.Rotational.updateRotationalAt(world, pos);
        NetworkComponent.Energy.updateEnergyAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof Rotational || block instanceof Energy;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ElectricMotorBlockEntity be) {
            be.updateRotationalData(modifier, state, world, pos);
        }
    }

    @Override
    public void updateEnergyData(EnergyData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ElectricMotorBlockEntity be) {
            be.updateEnergyData(modifier, state, world, pos);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown() ? ctx.getClickedFace() : ctx.getClickedFace().getOpposite());
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.getValue(FACING)));
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
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        //if (!player.isSneaking() && hand == Hand.MAIN_HAND && hit.getSide() != state.get(FACING) && player.isCreative() && world.getBlockEntity(pos) instanceof ElectricMotorBlockEntity be && player instanceof ServerPlayerEntity serverPlayer) {
        //    be.openGui(serverPlayer);
        //    return ActionResult.SUCCESS;
        //}
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricMotorBlockEntity(pos, state);
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.getValue(FACING).getOpposite()));
    }

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(FACING) == dir.getOpposite();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement axle;
        private final ItemDisplayElement base;

        public Model(BlockState state) {
            this.axle = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL_SHORT, this.getUpdateRate(), 0.3f, 0.6f);
            this.base = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.updateAnimation(0, state.getValue(FACING));
            this.addElement(this.axle);
            this.addElement(this.base);
        }

        private void updateAnimation(float speed, Direction facing) {
            var mat = mat();
            mat.rotate(facing.getOpposite().getRotation());
            mat.rotateY((facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) ? speed : -speed);

            mat.scale(2f);
            this.axle.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                var facing = this.blockState().getValue(FACING);

                this.updateAnimation(this.getRotation(), facing);
                this.axle.startInterpolationIfDirty();
            }
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
