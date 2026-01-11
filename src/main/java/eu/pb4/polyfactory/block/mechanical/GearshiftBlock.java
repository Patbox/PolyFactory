package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.nodes.generic.SimpleDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.HalfAxleWithSelfGearMechanicalNode;
import eu.pb4.polyfactory.nodes.mechanical_connectors.SelfGearNode;
import eu.pb4.polyfactory.nodes.mechanical_connectors.SmallGearNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

public class GearshiftBlock extends ClutchBlock implements NetworkComponent.RotationalConnector {
    public static final Property<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;

    public GearshiftBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false).setValue(POWERED, false).setValue(INVERTED, false));
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        var ax = state.getValue(AXIS);
        return List.of(
                new HalfAxleWithSelfGearMechanicalNode(ax.getPositive()),
                new HalfAxleWithSelfGearMechanicalNode(ax.getNegative())
        );
    }

    @Override
    public Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerLevel world, BlockPos pos) {
        var axis = state.getValue(AXIS);
        var flip = state.getValue(POWERED) == state.getValue(INVERTED);
        return List.of(new SelfGearNode(axis.getPositive(), flip), new SelfGearNode(axis.getNegative(), flip));
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        super.updateNetworkAt(world, pos);
        NetworkComponent.RotationalConnector.updateRotationalConnectorAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return super.isSameNetworkType(block) || block instanceof RotationalConnector;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.COPPER_BLOCK.defaultBlockState();
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    public final class Model extends ClutchBlock.Model {
        private Model(ServerLevel world, BlockState state) {
            super(world, state);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                var left = this.getRotationData(this::isNegative).rotation();
                var right = this.getRotationData(this::isPositive).rotation();

                this.updateAnimation(left, right, this.blockState().getValue(AXIS));

                this.left.startInterpolationIfDirty();
                this.right.startInterpolationIfDirty();
            }
        }
    }
}
