package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
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
import net.minecraft.item.Equipment;
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
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public class TinyPotatoSpringBlock extends DataNetworkBlock implements FactoryBlock, CableConnectable, BarrierBasedWaterloggable, Equipment {
    public static final Identifier STATISTIC = PolymerStat.registerStat(id("taters_clicked"), StatFormatter.DEFAULT);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public TinyPotatoSpringBlock(Settings settings) {
        super(settings.ticksRandomly());
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        var holder = BlockBoundAttachment.get(world, hit.getBlockPos());

        if (holder != null && holder.holder() instanceof Model x) {
            x.interact(projectile.getYaw());
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

            return ActionResult.SUCCESS;

    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()));
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.OAK_PLANKS.getDefaultState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
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

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public RegistryEntry<SoundEvent> getEquipSound() {
        return Equipment.super.getEquipSound();
    }

    @Override
    public TypedActionResult<ItemStack> equipAndSwap(Item item, World world, PlayerEntity user, Hand hand) {
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    public static final class Model extends BlockModel {
        public static final ItemStack BASE_MODEL = BaseItemProvider.requestModel(id("block/tiny_potato_spring_base"));
        public static final ItemStack TATER_MODEL = BaseItemProvider.requestModel(id("block/tiny_potato_spring"));
        private final ItemDisplayElement base;
        private final LodItemDisplayElement tater;
        private float extraRotation = 0;

        private int animationTimer = -1;
        private float interactionYaw;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.base = ItemDisplayElementUtil.createSimple(BASE_MODEL);
            this.tater = LodItemDisplayElement.createSimple(TATER_MODEL, 1);
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
            float y = dir.asRotation();

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
