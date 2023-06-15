package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.network.RotationalNetworkBlock;
import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.display.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.mechanical.GearboxNode;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class GearboxBlock extends RotationalNetworkBlock implements PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public GearboxBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new GearboxNode());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.SMOOTH_STONE.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model();
    }

    public final class Model extends LodElementHolder {
        private final ItemDisplayElement mainElement;

        private Model() {
            this.mainElement = new LodItemDisplayElement(FactoryItems.GEARBOX_BLOCK.getDefaultStack());
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setScale(new Vector3f(2));
            this.addElement(this.mainElement);
        }
    }
}
