package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.mixin.SettingsAccessor;
import eu.pb4.polyfactory.util.PolyFactoryConfig;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import static eu.pb4.polyfactory.ModInit.id;

public abstract class SimpleFastBlock extends Block implements PolymerBlock {
    public SimpleFastBlock(Settings settings) {
        super(settings);
    }

    public static SimpleFastBlock create(Settings settings) {
        if (PolymerBlockResourceUtils.getBlocksLeft(BlockModelType.FULL_BLOCK) > 0 && PolyFactoryConfig.get().useFastFullBlocks) {
            return new TexturedBlock(settings);
        }

        return new VirtualBlock(settings);
    }

    private static class TexturedBlock extends SimpleFastBlock implements PolymerTexturedBlock {
        private final BlockState state;
        public TexturedBlock(Settings settings) {
            super(settings);
            this.state = PolymerBlockResourceUtils.requestBlock(BlockModelType.FULL_BLOCK, PolymerBlockModel.of(id("block/steel_block")));
             //       ((SettingsAccessor) settings).getRegistryKey().getValue().withPrefixedPath("block/")));
        }

        @Override
        public BlockState getPolymerBlockState(BlockState blockState) {
            return this.state;
        }
    }

    private static class VirtualBlock extends SimpleFastBlock implements FactoryBlock {
        public VirtualBlock(Settings settings) {
            super(settings);
        }

        @Override
        public BlockState getPolymerBlockState(BlockState blockState) {
            return Blocks.BARRIER.getDefaultState();
        }

        @Override
        public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
            var model = new BlockModel();
            var element = ItemDisplayElementUtil.createSimple(this.asItem());
            element.setScale(new Vector3f(2));
            model.addElement(element);
            return model;
        }
    }
}
