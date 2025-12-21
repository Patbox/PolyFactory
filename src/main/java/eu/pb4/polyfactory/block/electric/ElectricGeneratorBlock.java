package eu.pb4.polyfactory.block.electric;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.AxisAndFacingNetworkBlock;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class ElectricGeneratorBlock extends AxisAndFacingNetworkBlock implements FactoryBlock, CableConnectable, RotationUser, EnergyUser  {
    public ElectricGeneratorBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        Rotational.updateRotationalAt(world, pos);
        Energy.updateEnergyAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof Rotational || block instanceof Energy;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        modifier.stress(15);
    }

    @Override
    public void updateEnergyData(EnergyData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        var speed = RotationUser.getRotation(world, pos).speed();

        modifier.provide((long) (speed * 15));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
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
        return List.of(new FunctionalAxisNode(getAxis(state)));
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
        if (!player.isShiftKeyDown() && hit.getDirection() != state.getValue(FACING) && player.isCreative() && world.getBlockEntity(pos) instanceof ElectricMotorBlockEntity be && player instanceof ServerPlayer serverPlayer) {
            be.openGui(serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalDirectionNode(state.getValue(FACING)));
    }

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(FACING) == dir;
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement axle;
        private final ItemDisplayElement base;

        public Model(BlockState state) {
            this.axle = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.base = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.updateAnimation(0, state);
            this.addElement(this.axle);
            this.addElement(this.base);
        }

        private void updateAnimation(float speed, BlockState state) {
            var mat = mat();
            switch (getAxis(state)) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
            }

            mat.rotateY(speed);

            mat.scale(2, 2f, 2);
            this.axle.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                var facing = this.blockState();

                this.updateAnimation(this.getRotation(), facing);
                this.axle.startInterpolationIfDirty();
            }
        }

        private void updateStatePos(BlockState state) {
            var mat = mat();
            mat.rotate(state.getValue(FIRST_AXIS) ? Mth.HALF_PI : 0, state.getValue(FACING).step());
            mat.rotate(state.getValue(FACING).getRotation());
            mat.scale(2f);
            this.base.setTransformation(mat);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                updateAnimation(this.getRotation(), this.blockState());
                this.axle.setInterpolationDuration(0);
                this.axle.tick();
                this.axle.setInterpolationDuration(this.getUpdateRate());
            }
        }
    }
}
