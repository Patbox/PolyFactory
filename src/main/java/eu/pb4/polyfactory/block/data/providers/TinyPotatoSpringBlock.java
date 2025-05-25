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
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.mixin.SettingsAccessor;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.util.PotatoWisdom;
import eu.pb4.polymer.core.api.other.PolymerStat;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public class TinyPotatoSpringBlock extends DataNetworkBlock implements FactoryBlock, CableConnectable, BarrierBasedWaterloggable {
    public static final Identifier STATISTIC = PolymerStat.registerStat(id("taters_clicked"), StatFormatter.DEFAULT);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private final Identifier taterModelId;
    private final Identifier baseModelId;


    public TinyPotatoSpringBlock(Settings settings) {
        super(settings.ticksRandomly());
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
        var id = ((SettingsAccessor) settings).getRegistryKey().getValue();
        this.taterModelId = id.withPrefixedPath("block/");
        this.baseModelId = this.taterModelId.withSuffixedPath("_base");
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        var holder = BlockBoundAttachment.get(world, hit.getBlockPos());

        if (holder != null && holder.holder() instanceof Model x) {
            var delta = hit.getPos().subtract(Vec3d.ofCenter(hit.getBlockPos()));
            var angle = Math.atan2(delta.x, -delta.z);
            x.interact((float) angle * MathHelper.DEGREES_PER_RADIAN);
        }
    }

    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        var holder = BlockBoundAttachment.get(world, pos);

        if (holder != null && holder.holder() instanceof Model x) {
            x.interact(player.getYaw());
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
            var holder = BlockBoundAttachment.get(world, pos);

            if (holder != null && holder.holder() instanceof Model x) {
                x.interact(player.getYaw());
            }
            if (player instanceof ServerPlayerEntity serverPlayer) {
                player.incrementStat(STATISTIC);

                switch (serverPlayer.getStatHandler().getStat(Stats.CUSTOM, STATISTIC)) {
                    case 16 -> TriggerCriterion.trigger(serverPlayer, FactoryTriggers.TATER_16);
                    case 128 -> TriggerCriterion.trigger(serverPlayer, FactoryTriggers.TATER_128);
                    case 1024 -> TriggerCriterion.trigger(serverPlayer, FactoryTriggers.TATER_1024);
                }
            }

            return ActionResult.SUCCESS_SERVER;

    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()));
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.OAK_PLANKS.getDefaultState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return dir == Direction.DOWN;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        DataProvider.sendData(world, pos, 0, Direction.DOWN, new StringData(PotatoWisdom.get(random)));
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelProviderDirectionNode(Direction.DOWN, 0));
    }

    public class Model extends BlockModel {
        private final ItemDisplayElement base;
        private final LodItemDisplayElement tater;
        private float extraRotation = 0;

        private int animationTimer = -1;
        private float interactionYaw;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.base = ItemDisplayElementUtil.createSimple(baseModelId);
            this.tater = LodItemDisplayElement.createSimple(taterModelId, 1);
            this.tater.setTranslation(new Vector3f(0, -6f / 16, 0));
            //this.tater.setOffset(new Vec3d(0, 2f / 16, 0));

            this.updateRotation(state.get(FACING));

            this.addElement(this.base);
            this.addElement(this.tater);
        }

        public void interact(float playerYaw) {
            this.interactionYaw = -playerYaw * MathHelper.RADIANS_PER_DEGREE;
            this.animationTimer = 60;
        }

        @Override
        protected void onTick() {
            if (this.animationTimer >= 0) {
                var q = new Quaternionf();
                var yaw = this.extraRotation + this.interactionYaw - MathHelper.HALF_PI;
                q.rotateAxis(MathHelper.sin(this.animationTimer * 0.5f) * MathHelper.lerp(this.animationTimer / 60f, 0, 0.4f),
                        MathHelper.sin(yaw), 0, MathHelper.cos(yaw));
                this.tater.setLeftRotation(q);
                this.tater.startInterpolation();
                this.animationTimer--;
            }

            super.onTick();
        }

        private void updateRotation(Direction dir) {
            float y = dir.getPositiveHorizontalDegrees();

            this.base.setYaw(y);
            this.tater.setYaw(y);
            this.extraRotation = y * MathHelper.RADIANS_PER_DEGREE;
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateRotation(this.blockState().get(FACING));
            }
        }
    }
}
