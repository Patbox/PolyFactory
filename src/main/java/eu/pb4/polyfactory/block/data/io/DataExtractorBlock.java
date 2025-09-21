package eu.pb4.polyfactory.block.data.io;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.BlockValueFormatter;
import eu.pb4.polyfactory.block.data.InputTransformerBlock;
import eu.pb4.polyfactory.block.data.InputTransformerBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DataExtractorBlock extends InputTransformerBlock {
    public static final List<BlockConfig<?>> BLOCK_CONFIG = ImmutableList.<BlockConfig<?>>builder()
            .addAll(InputTransformerBlock.BLOCK_CONFIG)
            .add(BlockConfig.ofBlockEntity("data_extractor.field", Codec.STRING, DataExtractorBlockEntity.class, BlockValueFormatter.getDefault(),
                    DataExtractorBlockEntity::field, DataExtractorBlockEntity::setField,
                    (value, next, player, world, pos, side, state) -> {
                        if (player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof DataExtractorBlockEntity be) {
                            be.openGui(serverPlayer);
                            serverPlayer.swingHand(Hand.MAIN_HAND, true);
                        }
                        return value;
                    })
            ).build();

    public DataExtractorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DataExtractorBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player instanceof ServerPlayerEntity serverPlayer && !player.shouldCancelInteraction() && world.getBlockEntity(pos) instanceof DataExtractorBlockEntity be) {
            be.openGui(serverPlayer);
            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected DataContainer transformData(DataContainer input, ServerWorld world, BlockPos selfPos, BlockState selfState, InputTransformerBlockEntity be) {
        return input.extract(((DataExtractorBlockEntity) be).field());
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return BLOCK_CONFIG;
    }
}
