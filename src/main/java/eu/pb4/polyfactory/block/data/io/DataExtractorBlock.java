package eu.pb4.polyfactory.block.data.io;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.BlockValueFormatter;
import eu.pb4.polyfactory.block.data.InputTransformerBlock;
import eu.pb4.polyfactory.block.data.InputTransformerBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DataExtractorBlock extends InputTransformerBlock {
    public static final List<BlockConfig<?>> BLOCK_CONFIG = ImmutableList.<BlockConfig<?>>builder()
            .addAll(InputTransformerBlock.BLOCK_CONFIG)
            .add(BlockConfig.ofBlockEntity("data_extractor.field", Codec.STRING, DataExtractorBlockEntity.class, BlockValueFormatter.getDefault(),
                    DataExtractorBlockEntity::field, DataExtractorBlockEntity::setField,
                    (value, next, player, world, pos, side, state) -> {
                        if (player instanceof ServerPlayer serverPlayer && world.getBlockEntity(pos) instanceof DataExtractorBlockEntity be) {
                            be.openGui(serverPlayer);
                            serverPlayer.swing(InteractionHand.MAIN_HAND, true);
                        }
                        return value;
                    })
            ).build();

    public DataExtractorBlock(Properties settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataExtractorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && !player.isSecondaryUseActive() && world.getBlockEntity(pos) instanceof DataExtractorBlockEntity be) {
            be.openGui(serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected DataContainer transformData(DataContainer input, ServerLevel world, BlockPos selfPos, BlockState selfState, InputTransformerBlockEntity be) {
        return input.extract(((DataExtractorBlockEntity) be).field());
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return BLOCK_CONFIG;
    }
}
