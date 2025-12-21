package eu.pb4.polyfactory.block.data.io;

import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlock;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlockEntity;
import eu.pb4.polyfactory.block.data.InputTransformerBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ProgrammableDataExtractorBlock extends DoubleInputTransformerBlock {
    public ProgrammableDataExtractorBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected DataContainer transformData(DataContainer input1, DataContainer input2, ServerLevel world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be) {
        return input1.extract(input2.asString());
    }
}
