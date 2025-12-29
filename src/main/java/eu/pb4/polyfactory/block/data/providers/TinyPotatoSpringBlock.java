package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.mixin.PropertiesAccessor;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.util.PotatoWisdom;
import eu.pb4.polymer.core.api.other.PolymerStat;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static eu.pb4.polyfactory.ModInit.id;

public class TinyPotatoSpringBlock extends DataNetworkBlock implements FactoryBlock, CableConnectable, BarrierBasedWaterloggable {
    public static final Identifier STATISTIC = PolymerStat.registerStat(id("taters_clicked"), StatFormatter.DEFAULT);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final Identifier taterModelId;
    private final Identifier baseModelId;


    public TinyPotatoSpringBlock(Properties settings) {
        super(settings.randomTicks());
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
        var id = ((PropertiesAccessor) settings).getId().identifier();
        this.taterModelId = id.withPrefix("block/");
        this.baseModelId = this.taterModelId.withSuffix("_base");
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public void onProjectileHit(Level world, BlockState state, BlockHitResult hit, Projectile projectile) {
        var holder = BlockBoundAttachment.get(world, hit.getBlockPos());

        if (holder != null && holder.holder() instanceof Model x) {
            var delta = hit.getLocation().subtract(Vec3.atCenterOf(hit.getBlockPos()));
            var angle = Math.atan2(delta.x, -delta.z);
            x.interact((float) angle * Mth.RAD_TO_DEG);
        }
    }

    @Override
    public void attack(BlockState state, Level world, BlockPos pos, Player player) {
        var holder = BlockBoundAttachment.get(world, pos);

        if (holder != null && holder.holder() instanceof Model x) {
            x.interact(player.getYRot());
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
            var holder = BlockBoundAttachment.get(world, pos);

            if (holder != null && holder.holder() instanceof Model x) {
                x.interact(player.getYRot());
            }
            if (player instanceof ServerPlayer serverPlayer) {
                player.awardStat(STATISTIC);

                switch (serverPlayer.getStats().getValue(Stats.CUSTOM, STATISTIC)) {
                    case 16 -> TriggerCriterion.trigger(serverPlayer, FactoryTriggers.TATER_16);
                    case 128 -> TriggerCriterion.trigger(serverPlayer, FactoryTriggers.TATER_128);
                    case 1024 -> TriggerCriterion.trigger(serverPlayer, FactoryTriggers.TATER_1024);
                }
            }

            return InteractionResult.SUCCESS_SERVER;

    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()));
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return dir == Direction.DOWN;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        DataProvider.sendData(world, pos, 0, Direction.DOWN, new StringData(PotatoWisdom.get(random)));
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new ChannelProviderDirectionNode(Direction.DOWN, 0));
    }

    public class Model extends BlockModel {
        private final ItemDisplayElement base;
        private final LodItemDisplayElement tater;
        private float extraRotation = 0;

        private int animationTimer = -1;
        private float interactionYaw;

        private Model(ServerLevel world, BlockPos pos, BlockState state) {
            this.base = ItemDisplayElementUtil.createSolid(baseModelId);
            this.tater = LodItemDisplayElement.createSolid(taterModelId, 1);
            this.tater.setTranslation(new Vector3f(0, -6f / 16, 0));
            //this.tater.setOffset(new Vec3d(0, 2f / 16, 0));

            this.updateRotation(state.getValue(FACING));

            this.addElement(this.base);
            this.addElement(this.tater);
        }

        public void interact(float playerYaw) {
            this.interactionYaw = -playerYaw * Mth.DEG_TO_RAD;
            this.animationTimer = 60;
        }

        @Override
        protected void onTick() {
            if (this.animationTimer >= 0) {
                var q = new Quaternionf();
                var yaw = this.extraRotation + this.interactionYaw - Mth.HALF_PI;
                q.rotateAxis(Mth.sin(this.animationTimer * 0.5f) * Mth.lerp(this.animationTimer / 60f, 0, 0.4f),
                        Mth.sin(yaw), 0, Mth.cos(yaw));
                this.tater.setLeftRotation(q);
                this.tater.startInterpolationIfDirty();
                this.animationTimer--;
            }

            super.onTick();
        }

        private void updateRotation(Direction dir) {
            float y = dir.toYRot();

            this.base.setYaw(y);
            this.tater.setYaw(y);
            this.extraRotation = y * Mth.DEG_TO_RAD;
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateRotation(this.blockState().getValue(FACING));
            }
        }
    }
}
