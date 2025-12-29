package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.mixin.PropertiesAccessor;
import eu.pb4.polyfactory.util.PolyFactoryConfig;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public abstract class SimpleFastBlock extends Block implements PolymerBlock {
    public SimpleFastBlock(Properties settings) {
        super(settings);
    }

    public static SimpleFastBlock create(Properties settings) {
        if (PolymerBlockResourceUtils.getBlocksLeft(BlockModelType.FULL_BLOCK) > 0 && PolyFactoryConfig.get().useFastFullBlocks) {
            return new TexturedBlock(settings);
        }

        return new VirtualBlock(settings);
    }

    private static class TexturedBlock extends SimpleFastBlock implements PolymerTexturedBlock {
        private final BlockState state;
        public TexturedBlock(Properties settings) {
            super(settings);
            this.state = PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(
                    ((PropertiesAccessor) settings).getId().identifier().withPrefix("block/")));
        }

        @Override
        public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
            return this.state;
        }
    }

    private static class VirtualBlock extends SimpleFastBlock implements FactoryBlock {
        public VirtualBlock(Properties settings) {
            super(settings);
        }

        @Override
        public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
            return Blocks.BARRIER.defaultBlockState();
        }

        @Override
        public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
            var model = new BlockModel();
            var element = ItemDisplayElementUtil.createSolid(this.asItem());
            element.setScale(new Vector3f(2));
            model.addElement(element);
            return model;
        }
    }
}
