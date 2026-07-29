package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.CustomBreakingParticleBlock;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.util.LazyItemStack;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class CheeseBlock extends Block implements FactoryBlock, PolymerTexturedBlock, CustomBreakingParticleBlock {
    private static final BlockState POLYMER_STATE = PolymerBlockResourceUtils.requestEmpty(BlockModelType.CAMPFIRE);
    public static final IntegerProperty BITES = BlockStateProperties.BITES;
    private final LazyItemStack[] models;
    private final Supplier<ItemParticleOption> breakingParticle;

    public CheeseBlock(final BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(BITES, 0));

        var id = properties.blockIdOrThrow().identifier().withPrefix("block/");
        this.models = new LazyItemStack[]{
                ItemDisplayElementUtil.getModel(id),
                ItemDisplayElementUtil.getModel(id.withSuffix("_slice1")),
                ItemDisplayElementUtil.getModel(id.withSuffix("_slice2")),
                ItemDisplayElementUtil.getModel(id.withSuffix("_slice3")),
                ItemDisplayElementUtil.getModel(id.withSuffix("_slice4")),
                ItemDisplayElementUtil.getModel(id.withSuffix("_slice5")),
                ItemDisplayElementUtil.getModel(id.withSuffix("_slice6")),
        };
        this.breakingParticle = ItemDisplayElementUtil.getModel(id).derivative(x -> new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(x)));
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        } else if (level instanceof ServerLevel serverLevel) {
            //player.awardStat(Stats.EAT_CAKE_SLICE);
            player.getFoodData().eat(2, 0.5F);
            int bites = state.getValue(BITES);
            level.gameEvent(player, GameEvent.EAT, pos);

            for (int i = 0; i < 4; i++) {
                Vec3 d = new Vec3((player.getRandom().nextFloat() - 0.5) * 0.1, player.getRandom().nextFloat() * 0.1 + 0.1, 0.0);
                d = d.xRot(-player.getXRot() * (float) (Math.PI / 180.0));
                d = d.yRot(-player.getYRot() * (float) (Math.PI / 180.0));
                double y1 = -player.getRandom().nextFloat() * 0.6 - 0.3;
                Vec3 p = new Vec3((player.getRandom().nextFloat() - 0.5) * 0.3, y1, 0.6);
                p = p.xRot(-player.getXRot() * (float) (Math.PI / 180.0));
                p = p.yRot(-player.getYRot() * (float) (Math.PI / 180.0));
                p = p.add(player.getX(), player.getEyeY(), player.getZ());
                serverLevel.sendParticles(this.breakingParticle.get(), p.x, p.y, p.z, 0, d.x, d.y + 0.05, d.z, 1);
            }


            if (bites < 6) {
                level.setBlock(pos, state.setValue(BITES, bites + 1), 3);
                level.playSound(null, player, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.5f, 1 + player.getRandom().nextFloat() * 0.4f - 0.2f);
            } else {
                level.removeBlock(pos, false);
                level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
                level.playSound(null, player, SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5f, 1 + player.getRandom().nextFloat() * 0.4f - 0.2f);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BITES);
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
        return getOutputSignal(state.getValue(BITES));
    }

    public static int getOutputSignal(final int bitesTaken) {
        return (7 - bitesTaken) * 2;
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
        return false;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, @Nullable PacketContext context) {
        return POLYMER_STATE;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public ParticleOptions getBreakingParticle(BlockState state) {
        return this.breakingParticle.get();
    }

    public class Model extends BlockModel {
        private final ItemDisplayElement main;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple();
            this.main.setYaw(180);
            this.updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            this.main.setItem(models[state.getValue(BITES)].get());
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                this.tick();
            }
        }
    }
}