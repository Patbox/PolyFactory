package eu.pb4.polyfactory.block.other;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.models.BaseItemProvider;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.other.PolymerStat;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.StatType;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static eu.pb4.polyfactory.ModInit.id;

public class TinyPotatoSpringBlock extends Block implements PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public static final Identifier STATISTIC = PolymerStat.registerStat(id("taters_clicked"), StatFormatter.DEFAULT);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public TinyPotatoSpringBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.MAIN_HAND) {
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

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
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
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public static final class Model extends BaseModel {
        public static final ItemStack BASE_MODEL = BaseItemProvider.requestModel(id("block/tiny_potato_spring_base"));
        public static final ItemStack TATER_MODEL = BaseItemProvider.requestModel(id("block/tiny_potato_spring"));
        private final LodItemDisplayElement base;
        private final LodItemDisplayElement tater;
        private float extraRotation = 0;

        private int animationTimer = -1;
        private float interactionYaw;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.base = LodItemDisplayElement.createSimple(BASE_MODEL);
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
                updateRotation(this.blockBound().getBlockState().get(FACING));
            }
        }
    }
}
