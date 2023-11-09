package eu.pb4.polyfactory.block.data.output;

import eu.pb4.factorytools.api.block.FactoryBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;

public class NixieTubeConnectorBlock extends Block implements FactoryBlock {
    public static final BooleanProperty TOP_CONNECTOR = BooleanProperty.of("top_connector");
    public static final BooleanProperty BOTTOM_CONNECTOR = BooleanProperty.of("bottom_connector");
    public NixieTubeConnectorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(TOP_CONNECTOR);
        builder.add(BOTTOM_CONNECTOR);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }
}
